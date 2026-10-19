package com.delivery.shifts.web;

import com.delivery.shifts.domain.VehicleType;
import com.delivery.shifts.domain.Zone;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
@Tag(name = "Metadata")
public class MetaController {

    /** Reference values used to populate dropdowns. */
    @GetMapping
    public Map<String, List<String>> meta() {
        return Map.of(
                "zones", List.of(Zone.values()).stream().map(Enum::name).toList(),
                "vehicleTypes", List.of(VehicleType.values()).stream().map(Enum::name).toList()
        );
    }
}
