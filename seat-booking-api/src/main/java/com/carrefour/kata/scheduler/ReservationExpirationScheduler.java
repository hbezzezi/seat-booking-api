package com.carrefour.kata.scheduler;

import com.carrefour.kata.domain.Reservation;
import com.carrefour.kata.domain.ReservationStatus;
import com.carrefour.kata.domain.SeatStatus;
import com.carrefour.kata.repository.ReservationRepository;
import com.carrefour.kata.repository.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class ReservationExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    public ReservationExpirationScheduler(ReservationRepository reservationRepository,
                                          SeatRepository seatRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
    }

    @Scheduled(fixedRate = 60000) // toutes les 60 secondes
    @Transactional
    public void expireReservations() {

        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndExpiresAtBefore(
                        ReservationStatus.PENDING,
                        LocalDateTime.now()
                );

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.getSeat().setStatus(SeatStatus.AVAILABLE);
            reservationRepository.save(reservation);
        }
    }
}

