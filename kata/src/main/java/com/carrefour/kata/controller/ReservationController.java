package com.carrefour.kata.controller;

import com.carrefour.kata.domain.Event;
import com.carrefour.kata.domain.Reservation;
import com.carrefour.kata.dto.*;
import com.carrefour.kata.service.EventService;
import com.carrefour.kata.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;
    private final EventService eventService;
    private final ReservationMapper mapper;

    public ReservationController(ReservationService reservationService,
                                 EventService eventService,
                                 ReservationMapper mapper) {
        this.reservationService = reservationService;
        this.eventService = eventService;
        this.mapper = mapper;
    }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request) {
        Event event = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toEventResponse(event));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toEventResponse(eventService.getEvent(id)));
    }

    @PostMapping("/events/{eventId}/reservations")
    public ResponseEntity<ReservationResponse> hold(
            @PathVariable Long eventId,
            @Valid @RequestBody HoldRequest request) {
        Reservation reservation = reservationService.hold(
                request.getSeatId(),
                request.getCustomerEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toReservationResponse(reservation));
    }

    @PostMapping("/reservations/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(
                mapper.toReservationResponse(reservationService.confirm(id))
        );
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
