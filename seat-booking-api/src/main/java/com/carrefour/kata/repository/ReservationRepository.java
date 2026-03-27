package com.carrefour.kata.repository;

import com.carrefour.kata.domain.Reservation;
import com.carrefour.kata.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Pour le scheduler d'expiration
    List<Reservation> findByStatusAndExpiresAtBefore(
            ReservationStatus status,
            LocalDateTime dateTime
    );
}

