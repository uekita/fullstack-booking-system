package com.delivery.shifts.service;

import com.delivery.shifts.domain.Booking;
import com.delivery.shifts.domain.BookingStatus;
import com.delivery.shifts.domain.Courier;
import com.delivery.shifts.domain.Shift;
import com.delivery.shifts.dto.BookingRequest;
import com.delivery.shifts.dto.BookingResponse;
import com.delivery.shifts.dto.RescheduleRequest;
import com.delivery.shifts.exception.BusinessRuleException;
import com.delivery.shifts.exception.NotFoundException;
import com.delivery.shifts.repository.BookingRepository;
import com.delivery.shifts.repository.ShiftRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShiftRepository shiftRepository;
    private final CourierService courierService;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, ShiftRepository shiftRepository,
                          CourierService courierService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.shiftRepository = shiftRepository;
        this.courierService = courierService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> list(Long courierId, Long shiftId, BookingStatus status) {
        Specification<Booking> spec = (root, q, cb) -> cb.conjunction();
        if (courierId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("courier").get("id"), courierId));
        }
        if (shiftId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("shift").get("id"), shiftId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        return bookingRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id) {
        return BookingResponse.from(find(id));
    }

    @Transactional
    public BookingResponse create(BookingRequest req) {
        Instant now = clock.instant();
        Courier courier = courierService.find(req.courierId());
        Shift shift = shiftRepository.findByIdForUpdate(req.shiftId())
                .orElseThrow(() -> new NotFoundException("Shift", req.shiftId()));

        if (!courier.isActive()) {
            throw new BusinessRuleException("Courier is inactive and cannot book shifts");
        }
        if (shift.hasStarted(now)) {
            throw new BusinessRuleException("Shift has already started and can no longer be booked");
        }
        if (bookingRepository.existsByCourierIdAndShiftIdAndStatus(courier.getId(), shift.getId(), BookingStatus.CONFIRMED)) {
            throw new BusinessRuleException("Courier has already booked this shift");
        }
        if (bookingRepository.courierHasOverlappingBooking(courier.getId(), shift.getStartTime(), shift.getEndTime())) {
            throw new BusinessRuleException("Courier already has a booking that overlaps with this shift");
        }
        long booked = bookingRepository.countByShiftIdAndStatus(shift.getId(), BookingStatus.CONFIRMED);
        if (booked >= shift.getCapacity()) {
            throw new BusinessRuleException("Shift is full");
        }

        return BookingResponse.from(bookingRepository.save(new Booking(courier, shift)));
    }

    @Transactional
    public BookingResponse reschedule(Long bookingId, RescheduleRequest req) {
        Instant now = clock.instant();

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking", bookingId));
        Shift target = shiftRepository.findByIdForUpdate(req.shiftId())
                .orElseThrow(() -> new NotFoundException("Shift", req.shiftId()));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking is cancelled and cannot be rescheduled");
        }
        if (booking.getShift().getId().equals(target.getId())) {
            throw new BusinessRuleException("Booking is already on this shift");
        }
        if (!booking.getCourier().isActive()) {
            throw new BusinessRuleException("Courier is inactive and cannot book shifts");
        }
        if (target.hasStarted(now)) {
            throw new BusinessRuleException("Shift has already started and can no longer be booked");
        }

        if (bookingRepository.courierHasOverlappingBooking(
                booking.getCourier().getId(), target.getStartTime(), target.getEndTime())) {
            throw new BusinessRuleException("Courier already has a booking that overlaps with this shift");
        }

        long booked = bookingRepository.countByShiftIdAndStatus(target.getId(), BookingStatus.CONFIRMED);
        if (booked >= target.getCapacity()) {
            throw new BusinessRuleException("Shift is full");
        }

        booking.reschedule(target);
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse cancel(Long id) {
        Instant now = clock.instant();
        Booking booking = find(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking is already cancelled");
        }
        if (booking.getShift().hasStarted(now)) {
            throw new BusinessRuleException("Shift has already started; booking can no longer be cancelled");
        }
        booking.cancel(now);
        return BookingResponse.from(booking);
    }

    private Booking find(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking", id));
    }
}
