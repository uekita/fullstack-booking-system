package com.delivery.shifts.service;

import com.delivery.shifts.domain.Courier;
import com.delivery.shifts.dto.CourierRequest;
import com.delivery.shifts.dto.CourierResponse;
import com.delivery.shifts.exception.BusinessRuleException;
import com.delivery.shifts.exception.NotFoundException;
import com.delivery.shifts.repository.BookingRepository;
import com.delivery.shifts.repository.CourierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CourierService {

    private final CourierRepository courierRepository;
    private final BookingRepository bookingRepository;

    public CourierService(CourierRepository courierRepository, BookingRepository bookingRepository) {
        this.courierRepository = courierRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<CourierResponse> list(Boolean active) {
        List<Courier> couriers = active == null
                ? courierRepository.findAllByOrderByNameAsc()
                : courierRepository.findAllByActiveOrderByNameAsc(active);
        return couriers.stream().map(CourierResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CourierResponse get(Long id) {
        return CourierResponse.from(find(id));
    }

    @Transactional
    public CourierResponse create(CourierRequest req) {
        String email = normalizeEmail(req.email());
        if (courierRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("A courier with email " + email + " already exists");
        }
        Courier courier = new Courier();
        apply(courier, req, email);
        courier.setActive(req.active() == null || req.active());
        return CourierResponse.from(courierRepository.save(courier));
    }

    @Transactional
    public CourierResponse update(Long id, CourierRequest req) {
        Courier courier = find(id);
        String email = normalizeEmail(req.email());
        if (courierRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessRuleException("A courier with email " + email + " already exists");
        }
        apply(courier, req, email);
        if (req.active() != null) {
            courier.setActive(req.active());
        }
        return CourierResponse.from(courier);
    }

    @Transactional
    public void delete(Long id) {
        Courier courier = find(id);
        if (bookingRepository.existsByCourierId(id)) {
            throw new BusinessRuleException("Courier has bookings and cannot be deleted. Deactivate it instead");
        }
        courierRepository.delete(courier);
    }

    Courier find(Long id) {
        return courierRepository.findById(id).orElseThrow(() -> new NotFoundException("Courier", id));
    }

    Courier findForUpdate(Long id) {
        return courierRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Courier", id));
    }

    private static void apply(Courier courier, CourierRequest req, String email) {
        courier.setName(req.name().trim());
        courier.setEmail(email);
        courier.setPhone(req.phone().trim());
        courier.setVehicleType(req.vehicleType());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
