package com.delivery.shifts.dto;

import com.delivery.shifts.domain.Courier;
import com.delivery.shifts.domain.VehicleType;

import java.time.Instant;

public record CourierResponse(
        Long id,
        String name,
        String email,
        String phone,
        VehicleType vehicleType,
        boolean active,
        Instant createdAt
) {
    public static CourierResponse from(Courier c) {
        return new CourierResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(),
                c.getVehicleType(), c.isActive(), c.getCreatedAt());
    }
}
