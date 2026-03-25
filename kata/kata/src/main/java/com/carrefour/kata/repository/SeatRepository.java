package com.carrefour.kata.repository;

import com.carrefour.kata.domain.Seat;
import com.carrefour.kata.domain.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // Trouver tous les sièges disponibles d'un événement
    List<Seat> findByEventIdAndStatus(Long eventId, SeatStatus status);
}

