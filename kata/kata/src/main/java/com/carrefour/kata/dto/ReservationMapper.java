package com.carrefour.kata.dto;

import com.carrefour.kata.domain.Event;
import com.carrefour.kata.domain.Reservation;
import com.carrefour.kata.domain.SeatStatus;
import com.carrefour.kata.dto.EventResponse;
import com.carrefour.kata.dto.ReservationResponse;
import com.carrefour.kata.dto.SeatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReservationMapper {

    public EventResponse toEventResponse(Event event) {
        List<SeatResponse> seatResponses = event.getSeats().stream()
                .map(seat -> new SeatResponse(
                        seat.getId(),
                        seat.getSeatNumber(),
                        seat.getStatus()
                ))
                .toList();

        int availableSeats = (int) event.getSeats().stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .count();

        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getTotalCapacity(),
                availableSeats,
                seatResponses
        );
    }

    public ReservationResponse toReservationResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSeat().getId(),
                reservation.getSeat().getSeatNumber(),
                reservation.getCustomerEmail(),
                reservation.getStatus(),
                reservation.getExpiresAt()
        );
    }
}

