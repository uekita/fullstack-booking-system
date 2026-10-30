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
        // 7–20 digits; separators (spaces, dashes) do not count toward the digits. Max 30 chars = column length.
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^(?=(?:\\D*\\d){7,20}\\D*$)\\+?[0-9][0-9\\- ]*$", message = "must be a valid phone number")
        String phone,
        @NotNull VehicleType vehicleType,
        Boolean active
) {
    public CourierRequest {
        // The name is stored trimmed, so validate the trimmed value.
        name = name == null ? null : name.trim();
    }
}
