package com.delivery.shifts.dto;

import com.delivery.shifts.domain.Zone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ShiftRequest(
        @NotNull Zone zone,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull @Min(1) @Max(50) Integer capacity
) {
}
