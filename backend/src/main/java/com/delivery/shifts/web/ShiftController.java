package com.delivery.shifts.web;

import com.delivery.shifts.domain.Zone;
import com.delivery.shifts.dto.BookingResponse;
import com.delivery.shifts.dto.ShiftRequest;
import com.delivery.shifts.dto.ShiftResponse;
import com.delivery.shifts.service.BookingService;
import com.delivery.shifts.service.ShiftService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@Tag(name = "Shifts")
public class ShiftController {

    private final ShiftService shiftService;
    private final BookingService bookingService;

    public ShiftController(ShiftService shiftService, BookingService bookingService) {
        this.shiftService = shiftService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<ShiftResponse> list(
            @RequestParam(required = false) Zone zone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endAfter) {
        return shiftService.list(zone, from, to, endAfter);
    }

    @GetMapping("/{id}")
    public ShiftResponse get(@PathVariable Long id) {
        return shiftService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse create(@Valid @RequestBody ShiftRequest request) {
        return shiftService.create(request);
    }

    @PutMapping("/{id}")
    public ShiftResponse update(@PathVariable Long id, @Valid @RequestBody ShiftRequest request) {
        return shiftService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        shiftService.delete(id);
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> bookings(@PathVariable Long id) {
        shiftService.get(id);
        return bookingService.list(null, id, null);
    }
}
