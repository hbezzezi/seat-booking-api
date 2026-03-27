package com.carrefour.kata.service;

import com.carrefour.kata.domain.Reservation;
import com.carrefour.kata.domain.ReservationStatus;
import com.carrefour.kata.domain.Seat;
import com.carrefour.kata.domain.SeatStatus;
import com.carrefour.kata.exception.SeatNotAvailableException;
import com.carrefour.kata.repository.ReservationRepository;
import com.carrefour.kata.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    //   Hold
    @Test
    void should_hold_seat_when_available() {
        Seat seat = new Seat();
        seat.setId(1L);
        seat.setStatus(SeatStatus.AVAILABLE);

        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reservation result = reservationService.hold(1L, "test@email.com");

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void should_throw_when_seat_is_held() {
        Seat seat = new Seat();
        seat.setId(1L);
        seat.setStatus(SeatStatus.HELD);

        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> reservationService.hold(1L, "test@email.com"))
                .isInstanceOf(SeatNotAvailableException.class);
    }

    @Test
    void should_throw_when_seat_is_booked() {
        Seat seat = new Seat();
        seat.setId(1L);
        seat.setStatus(SeatStatus.BOOKED);

        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> reservationService.hold(1L, "test@email.com"))
                .isInstanceOf(SeatNotAvailableException.class);
    }

    @Test
    void should_throw_when_seat_not_found() {
        when(seatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.hold(99L, "test@email.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seat not found");
    }

    //   Confirm
    @Test
    void should_confirm_reservation_when_pending() {
        Seat seat = new Seat();
        seat.setStatus(SeatStatus.HELD);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setSeat(seat);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reservation result = reservationService.confirm(1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void should_throw_when_confirming_expired_reservation() {
        Seat seat = new Seat();
        seat.setStatus(SeatStatus.HELD);

        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setSeat(seat);
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirm(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void should_throw_when_confirming_non_pending_reservation() {
        Seat seat = new Seat();
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setSeat(seat);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirm(1L))
                .isInstanceOf(RuntimeException.class);
    }

    //   Cancel
    @Test
    void should_cancel_reservation_and_release_seat() {
        Seat seat = new Seat();
        seat.setStatus(SeatStatus.HELD);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setSeat(seat);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        reservationService.cancel(1L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
}

