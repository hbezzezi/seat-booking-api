package com.carrefour.kata.dto;

import com.carrefour.kata.domain.ReservationStatus;
import java.time.LocalDateTime;

public class ReservationResponse {
    private Long id;
    private Long seatId;
    private String seatNumber;
    private String customerEmail;
    private ReservationStatus status;
    private LocalDateTime expiresAt;

    public ReservationResponse(Long id, Long seatId, String seatNumber,
                               String customerEmail, ReservationStatus status,
                               LocalDateTime expiresAt) {
        this.id = id;
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.customerEmail = customerEmail;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getSeatId() { return seatId; }
    public String getSeatNumber() { return seatNumber; }
    public String getCustomerEmail() { return customerEmail; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}

