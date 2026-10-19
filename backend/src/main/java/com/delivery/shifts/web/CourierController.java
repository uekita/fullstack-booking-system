package com.delivery.shifts.web;

import com.delivery.shifts.dto.BookingResponse;
import com.delivery.shifts.dto.CourierRequest;
import com.delivery.shifts.dto.CourierResponse;
import com.delivery.shifts.service.BookingService;
import com.delivery.shifts.service.CourierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/couriers")
@Tag(name = "Couriers")
public class CourierController {

    private final CourierService courierService;
    private final BookingService bookingService;

    public CourierController(CourierService courierService, BookingService bookingService) {
        this.courierService = courierService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<CourierResponse> list(@RequestParam(required = false) Boolean active) {
        return courierService.list(active);
    }

    @GetMapping("/{id}")
    public CourierResponse get(@PathVariable Long id) {
        return courierService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourierResponse create(@Valid @RequestBody CourierRequest request) {
        return courierService.create(request);
    }

    @PutMapping("/{id}")
    public CourierResponse update(@PathVariable Long id, @Valid @RequestBody CourierRequest request) {
        return courierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courierService.delete(id);
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> bookings(@PathVariable Long id) {
        courierService.get(id);
        return bookingService.list(id, null, null);
    }
}
