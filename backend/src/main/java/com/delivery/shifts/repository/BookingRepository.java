package com.delivery.shifts.repository;

import com.delivery.shifts.domain.Booking;
import com.delivery.shifts.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /** Locks the booking row so concurrent reschedules cannot race. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** The booking's courier id, without loading the booking: lets callers lock the courier before the booking. */
    @Query("select b.courier.id from Booking b where b.id = :id")
    Optional<Long> findCourierIdById(@Param("id") Long id);

    long countByShiftIdAndStatus(Long shiftId, BookingStatus status);

    boolean existsByCourierIdAndShiftIdAndStatus(Long courierId, Long shiftId, BookingStatus status);

    boolean existsByCourierId(Long courierId);

    boolean existsByShiftId(Long shiftId);

    @Query("""
            select count(b) > 0 from Booking b
            where b.courier.id = :courierId
              and b.status = com.delivery.shifts.domain.BookingStatus.CONFIRMED
              and b.shift.startTime < :end
              and b.shift.endTime > :start
            """)
    boolean courierHasOverlappingBooking(@Param("courierId") Long courierId,
                                         @Param("start") Instant start,
                                         @Param("end") Instant end);

    /** As {@link #courierHasOverlappingBooking}, ignoring one booking: the one being rescheduled. */
    @Query("""
            select count(b) > 0 from Booking b
            where b.courier.id = :courierId
              and b.id <> :excludeBookingId
              and b.status = com.delivery.shifts.domain.BookingStatus.CONFIRMED
              and b.shift.startTime < :end
              and b.shift.endTime > :start
            """)
    boolean courierHasOtherOverlappingBooking(@Param("courierId") Long courierId,
                                              @Param("excludeBookingId") Long excludeBookingId,
                                              @Param("start") Instant start,
                                              @Param("end") Instant end);
}
