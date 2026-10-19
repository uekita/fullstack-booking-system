package com.delivery.shifts.dto;

import com.delivery.shifts.domain.Booking;
import com.delivery.shifts.domain.BookingStatus;
import com.delivery.shifts.domain.Zone;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long courierId,
        String courierName,
        Long shiftId,
        Zone zone,
        Instant shiftStart,
        Instant shiftEnd,
        BookingStatus status,
        Instant createdAt,
        Instant cancelledAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getCourier().getId(), b.getCourier().getName(),
                b.getShift().getId(), b.getShift().getZone(), b.getShift().getStartTime(), b.getShift().getEndTime(),
                b.getStatus(), b.getCreatedAt(), b.getCancelledAt());
    }
}
