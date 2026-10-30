package com.delivery.shifts.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id", nullable = false)
    private Courier courier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Booking() {
    }

    public Booking(Courier courier, Shift shift) {
        this.courier = courier;
        this.shift = shift;
        this.status = BookingStatus.CONFIRMED;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            // Microseconds: the precision PostgreSQL stores, so the create response matches later reads.
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }

    public void cancel(Instant now) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public void reschedule(Shift newShift) {
        this.shift = newShift;
    }

    public Long getId() { return id; }
    public Courier getCourier() { return courier; }
    public Shift getShift() { return shift; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
