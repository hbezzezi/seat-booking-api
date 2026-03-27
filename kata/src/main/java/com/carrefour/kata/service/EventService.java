package com.carrefour.kata.service;

import com.carrefour.kata.domain.Event;
import com.carrefour.kata.domain.Seat;
import com.carrefour.kata.domain.SeatStatus;
import com.carrefour.kata.dto.CreateEventRequest;
import com.carrefour.kata.repository.EventRepository;
import com.carrefour.kata.repository.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    public EventService(EventRepository eventRepository,
                        SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
    }

    public Event createEvent(CreateEventRequest request) {

        Event event = new Event();
        event.setName(request.getName());
        event.setTotalCapacity(request.getTotalCapacity());
        eventRepository.save(event);

// Générer automatiquement les sièges
        for (int i = 1; i <= request.getTotalCapacity(); i++) {
            Seat seat = new Seat();
            seat.setSeatNumber("S" + i);
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setEvent(event);
            seatRepository.save(seat);
        }

        return event;
    }

    public Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }
}

