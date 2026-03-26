package com.carrefour.kata.dto;

import java.util.List;

public class EventResponse {
    private Long id;
    private String name;
    private int totalCapacity;
    private int availableSeats;
    private List<SeatResponse> seats;

    // Constructeur
    public EventResponse(Long id, String name, int totalCapacity,
                         int availableSeats, List<SeatResponse> seats) {
        this.id = id;
        this.name = name;
        this.totalCapacity = totalCapacity;
        this.availableSeats = availableSeats;
        this.seats = seats;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getTotalCapacity() { return totalCapacity; }
    public int getAvailableSeats() { return availableSeats; }
    public List<SeatResponse> getSeats() { return seats; }
}


