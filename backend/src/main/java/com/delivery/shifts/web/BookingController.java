package com.delivery.shifts.web;

import com.delivery.shifts.domain.BookingStatus;
import com.delivery.shifts.dto.BookingRequest;
import com.delivery.shifts.dto.BookingResponse;
import com.delivery.shifts.dto.RescheduleRequest;
import com.delivery.shifts.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<BookingResponse> list(@RequestParam(required = false) Long courierId,
                                      @RequestParam(required = false) Long shiftId,
                                      @RequestParam(required = false) BookingStatus status) {
        return bookingService.list(courierId, shiftId, status);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        return bookingService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

    @PostMapping("/{id}/reschedule")
    public BookingResponse reschedule(@PathVariable Long id,
                                      @Valid @RequestBody RescheduleRequest request) {
        return bookingService.reschedule(id, request);
    }
}
