package com.delivery.shifts.dto;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull Long courierId,
        @NotNull Long shiftId
) {
}
