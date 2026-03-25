package com.carrefour.kata.service;

import com.carrefour.kata.domain.Event;
import com.carrefour.kata.domain.SeatStatus;
import com.carrefour.kata.dto.CreateEventRequest;
import com.carrefour.kata.repository.EventRepository;
import com.carrefour.kata.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void should_create_event_with_correct_seats() {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Concert Paris");
        request.setTotalCapacity(3);

        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Event result = eventService.createEvent(request);

        assertThat(result.getName()).isEqualTo("Concert Paris");
        assertThat(result.getTotalCapacity()).isEqualTo(3);

// Vérifie que 3 sièges ont été créés
        verify(seatRepository, times(3)).save(any());
    }

    @Test
    void should_create_seats_with_available_status() {
        CreateEventRequest request = new CreateEventRequest();
        request.setName("Concert Paris");
        request.setTotalCapacity(2);

        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<com.carrefour.kata.domain.Seat> seatCaptor =
                ArgumentCaptor.forClass(com.carrefour.kata.domain.Seat.class);

        when(seatRepository.save(seatCaptor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        eventService.createEvent(request);

        seatCaptor.getAllValues().forEach(seat ->
                assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE)
        );
    }

    @Test
    void should_throw_when_event_not_found() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Event not found");
    }
}

