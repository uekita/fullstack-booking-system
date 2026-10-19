package com.delivery.shifts.dto;

import com.delivery.shifts.domain.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourierRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9][0-9\\- ]{6,19}$", message = "must be a valid phone number") String phone,
        @NotNull VehicleType vehicleType,
        Boolean active
) {
}
