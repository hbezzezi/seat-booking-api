package com.carrefour.kata.controller;

import com.carrefour.kata.domain.Event;
import com.carrefour.kata.domain.Reservation;
import com.carrefour.kata.dto.CreateEventRequest;
import com.carrefour.kata.dto.HoldRequest;
import com.carrefour.kata.service.EventService;
import com.carrefour.kata.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;
    private final EventService eventService;

    public ReservationController(ReservationService reservationService,
                                 EventService eventService) {
        this.reservationService = reservationService;
        this.eventService = eventService;
    }

    // Créer un événement
    @PostMapping("/events")
    public ResponseEntity<Event> createEvent(@RequestBody CreateEventRequest request) {
        Event event = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    // Voir un événement et ses sièges
    @GetMapping("/events/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    // Réserver un siège (PENDING)
    @PostMapping("/events/{eventId}/reservations")
    public ResponseEntity<Reservation> hold(
            @PathVariable Long eventId,
            @RequestBody HoldRequest request) {

        Reservation reservation = reservationService.hold(
                request.getSeatId(),
                request.getCustomerEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    // Confirmer le paiement
    @PostMapping("/reservations/{id}/confirm")
    public ResponseEntity<Reservation> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    // Annuler
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}

