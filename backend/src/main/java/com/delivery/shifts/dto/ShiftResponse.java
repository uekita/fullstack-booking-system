package com.delivery.shifts.dto;

import com.delivery.shifts.domain.Shift;
import com.delivery.shifts.domain.Zone;

import java.time.Instant;

public record ShiftResponse(
        Long id,
        Zone zone,
        Instant startTime,
        Instant endTime,
        int capacity,
        long bookedCount,
        long availableSlots,
        ShiftStatus status,
        Instant createdAt
) {
    public enum ShiftStatus { OPEN, FULL, IN_PROGRESS, COMPLETED }

    public static ShiftResponse from(Shift s, long bookedCount, Instant now) {
        long available = Math.max(0, s.getCapacity() - bookedCount);
        ShiftStatus status;
        if (!s.getEndTime().isAfter(now)) {
            status = ShiftStatus.COMPLETED;
        } else if (s.hasStarted(now)) {
            status = ShiftStatus.IN_PROGRESS;
        } else if (available == 0) {
            status = ShiftStatus.FULL;
        } else {
            status = ShiftStatus.OPEN;
        }
        return new ShiftResponse(s.getId(), s.getZone(), s.getStartTime(), s.getEndTime(), s.getCapacity(),
                bookedCount, available, status, s.getCreatedAt());
    }
}
