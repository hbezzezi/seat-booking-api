package com.carrefour.kata.service;

import com.carrefour.kata.domain.Reservation;
import com.carrefour.kata.domain.ReservationStatus;
import com.carrefour.kata.domain.Seat;
import com.carrefour.kata.domain.SeatStatus;
import com.carrefour.kata.exception.SeatNotAvailableException;
import com.carrefour.kata.repository.ReservationRepository;
import com.carrefour.kata.repository.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class ReservationService {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(SeatRepository seatRepository,
                              ReservationRepository reservationRepository) {
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
    }

    //   Créer une réservation (PENDING)
    public Reservation hold(Long seatId, String customerEmail) {

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // Règle métier : le siège doit être disponible
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new SeatNotAvailableException("Seat is not available");
        }

        // Bloquer le siège
        seat.setStatus(SeatStatus.HELD);
        seatRepository.save(seat);

        // Créer la réservation
        Reservation reservation = new Reservation();
        reservation.setSeat(seat);
        reservation.setCustomerEmail(customerEmail);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        return reservationRepository.save(reservation);
    }

    //   Confirmer le paiement
    public Reservation confirm(Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // Règle métier : doit être PENDING et pas expirée
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("Reservation is not pending");
        }

        if (LocalDateTime.now().isAfter(reservation.getExpiresAt())) {
            throw new RuntimeException("Reservation has expired");
        }

        // Confirmer
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.getSeat().setStatus(SeatStatus.BOOKED);

        return reservationRepository.save(reservation);
    }

    //   Annuler
    public void cancel(Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.getSeat().setStatus(SeatStatus.AVAILABLE);

        reservationRepository.save(reservation);
    }
}

