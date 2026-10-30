package com.delivery.shifts.dto;

import jakarta.validation.constraints.NotNull;

public record RescheduleRequest(@NotNull Long shiftId) {
}
