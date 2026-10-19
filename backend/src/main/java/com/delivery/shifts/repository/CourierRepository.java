package com.delivery.shifts.repository;

import com.delivery.shifts.domain.Courier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierRepository extends JpaRepository<Courier, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Courier> findAllByOrderByNameAsc();

    List<Courier> findAllByActiveOrderByNameAsc(boolean active);
}
