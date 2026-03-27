package com.carrefour.kata.dto;

import com.carrefour.kata.domain.SeatStatus;

public class SeatResponse {
    private Long id;
    private String seatNumber;
    private SeatStatus status;

    public SeatResponse(Long id, String seatNumber, SeatStatus status) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.status = status;
    }

    // Getters
    public Long getId() { return id; }
    public String getSeatNumber() { return seatNumber; }
    public SeatStatus getStatus() { return status; }
}

