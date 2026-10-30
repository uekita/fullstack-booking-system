package com.delivery.shifts.repository;

import com.delivery.shifts.domain.Courier;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourierRepository extends JpaRepository<Courier, Long> {

    /** Locks the courier row so concurrent changes to one courier's bookings cannot race (overlap rule). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Courier c where c.id = :id")
    Optional<Courier> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Courier> findAllByOrderByNameAsc();

    List<Courier> findAllByActiveOrderByNameAsc(boolean active);
}
