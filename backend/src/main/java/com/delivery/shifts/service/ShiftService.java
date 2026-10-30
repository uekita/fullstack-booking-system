package com.delivery.shifts.service;

import com.delivery.shifts.domain.BookingStatus;
import com.delivery.shifts.domain.Shift;
import com.delivery.shifts.domain.Zone;
import com.delivery.shifts.dto.ShiftRequest;
import com.delivery.shifts.dto.ShiftResponse;
import com.delivery.shifts.exception.BusinessRuleException;
import com.delivery.shifts.exception.NotFoundException;
import com.delivery.shifts.exception.ValidationException;
import com.delivery.shifts.repository.BookingRepository;
import com.delivery.shifts.repository.ShiftRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ShiftService {

    static final Duration MIN_DURATION = Duration.ofHours(1);
    static final Duration MAX_DURATION = Duration.ofHours(12);

    private final ShiftRepository shiftRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public ShiftService(ShiftRepository shiftRepository, BookingRepository bookingRepository, Clock clock) {
        this.shiftRepository = shiftRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> list(Zone zone, Instant from, Instant to, Instant endAfter) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("'from' must be before 'to'");
        }
        Specification<Shift> spec = (root, q, cb) -> cb.conjunction();
        if (zone != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("zone"), zone));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("startTime"), to));
        }
        // On the end time, unlike from/to: endAfter=<now> keeps shifts in progress (not finished yet).
        if (endAfter != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThan(root.get("endTime"), endAfter));
        }
        Instant now = clock.instant();
        return shiftRepository.findAll(spec, Sort.by("startTime")).stream()
                .map(s -> toResponse(s, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftResponse get(Long id) {
        return toResponse(find(id), clock.instant());
    }

    @Transactional
    public ShiftResponse create(ShiftRequest req) {
        Instant now = clock.instant();
        validateTimes(req, now);
        Shift shift = new Shift();
        shift.setZone(req.zone());
        shift.setStartTime(req.startTime());
        shift.setEndTime(req.endTime());
        shift.setCapacity(req.capacity());
        return toResponse(shiftRepository.save(shift), now);
    }

    @Transactional
    public ShiftResponse update(Long id, ShiftRequest req) {
        Instant now = clock.instant();
        Shift shift = shiftRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Shift", id));
        if (shift.hasStarted(now)) {
            throw new BusinessRuleException("Shift has already started and can no longer be modified");
        }
        validateTimes(req, now);

        long booked = bookingRepository.countByShiftIdAndStatus(id, BookingStatus.CONFIRMED);
        boolean timesChanged = !shift.getStartTime().equals(req.startTime()) || !shift.getEndTime().equals(req.endTime());
        if (booked > 0 && timesChanged) {
            throw new BusinessRuleException("Shift times cannot be changed while it has confirmed bookings");
        }
        if (booked > 0 && shift.getZone() != req.zone()) {
            throw new BusinessRuleException("Shift zone cannot be changed while it has confirmed bookings");
        }
        if (req.capacity() < booked) {
            throw new BusinessRuleException("Capacity cannot be lower than the number of confirmed bookings (" + booked + ")");
        }

        shift.setZone(req.zone());
        shift.setStartTime(req.startTime());
        shift.setEndTime(req.endTime());
        shift.setCapacity(req.capacity());
        return ShiftResponse.from(shift, booked, now);
    }

    @Transactional
    public void delete(Long id) {
        Shift shift = find(id);
        if (bookingRepository.existsByShiftId(id)) {
            throw new BusinessRuleException("Shift has bookings and cannot be deleted");
        }
        shiftRepository.delete(shift);
    }

    Shift find(Long id) {
        return shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Shift", id));
    }

    private ShiftResponse toResponse(Shift s, Instant now) {
        return ShiftResponse.from(s, bookingRepository.countByShiftIdAndStatus(s.getId(), BookingStatus.CONFIRMED), now);
    }

    private static void validateTimes(ShiftRequest req, Instant now) {
        if (!req.startTime().isAfter(now)) {
            throw new ValidationException("Shift start time must be in the future");
        }
        if (!req.endTime().isAfter(req.startTime())) {
            throw new ValidationException("Shift end time must be after start time");
        }
        Duration duration = Duration.between(req.startTime(), req.endTime());
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new ValidationException("Shift duration must be between 1 and 12 hours");
        }
    }
}
