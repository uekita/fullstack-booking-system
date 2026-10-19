package com.delivery.shifts.repository;

import com.delivery.shifts.domain.Booking;
import com.delivery.shifts.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

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
}
