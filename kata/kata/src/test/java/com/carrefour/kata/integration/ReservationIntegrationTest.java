package com.carrefour.kata.integration;

import com.carrefour.kata.domain.Event;
import com.carrefour.kata.domain.Seat;
import com.carrefour.kata.domain.SeatStatus;
import com.carrefour.kata.dto.HoldRequest;
import com.carrefour.kata.repository.EventRepository;
import com.carrefour.kata.repository.ReservationRepository;
import com.carrefour.kata.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // charge application-test.properties
@Transactional // rollback après chaque test
class ReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Event event;
    private Seat seat;

    @BeforeEach
    void setUp() {
        // Nettoyer d'abord
        seatRepository.deleteAll();
        eventRepository.deleteAll();

        // Créer l'event
        event = new Event();
        event.setName("Concert Test");
        event.setTotalCapacity(3);
        eventRepository.save(event);

        // Créer le seat lié
        seat = new Seat();
        seat.setSeatNumber("S1");
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setEvent(event);
        seatRepository.save(seat);
    }


    // GET event
    @Test
    void should_return_event_with_seats() throws Exception {
        mockMvc.perform(get("/api/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Concert Test"))
                .andExpect(jsonPath("$.totalCapacity").value(3));
                //.andExpect(jsonPath("$.availableSeats").value(1));
    }

    // POST hold
    @Test
    void should_hold_seat_successfully() throws Exception {
        HoldRequest request = new HoldRequest();
        request.setSeatId(seat.getId());
        request.setCustomerEmail("john@example.com");

        mockMvc.perform(post("/api/events/" + event.getId() + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerEmail").value("john@example.com"))
                .andExpect(jsonPath("$.seatNumber").value("S1"));
    }

    // Siège déjà pris
    @Test
    void should_return_409_when_seat_already_held() throws Exception {
// Premier client réserve
        seat.setStatus(SeatStatus.HELD);
        seatRepository.save(seat);

// Deuxième client essaie
        HoldRequest request = new HoldRequest();
        request.setSeatId(seat.getId());
        request.setCustomerEmail("autre@example.com");

        mockMvc.perform(post("/api/events/" + event.getId() + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // Confirm
    @Test
    void should_confirm_reservation_successfully() throws Exception {
// D'abord créer une réservation
        HoldRequest request = new HoldRequest();
        request.setSeatId(seat.getId());
        request.setCustomerEmail("john@example.com");

        String holdResponse = mockMvc.perform(
                        post("/api/events/" + event.getId() + "/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

// Extraire l'ID de la réservation
        Long reservationId = objectMapper.readTree(holdResponse).get("id").asLong();

// Confirmer
        mockMvc.perform(post("/api/reservations/" + reservationId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // Cancel
    @Test
    void should_cancel_reservation_and_release_seat() throws Exception {
// Créer une réservation
        HoldRequest request = new HoldRequest();
        request.setSeatId(seat.getId());
        request.setCustomerEmail("john@example.com");

        String holdResponse = mockMvc.perform(
                        post("/api/events/" + event.getId() + "/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reservationId = objectMapper.readTree(holdResponse).get("id").asLong();

// Annuler
        mockMvc.perform(delete("/api/reservations/" + reservationId))
                .andExpect(status().isNoContent());

// Vérifier que le siège est redevenu AVAILABLE
        Seat updatedSeat = seatRepository.findById(seat.getId()).get();
        assert updatedSeat.getStatus() == SeatStatus.AVAILABLE;
    }

    // Validation
    @Test
    void should_return_400_when_email_invalid() throws Exception {
        HoldRequest request = new HoldRequest();
        request.setSeatId(seat.getId());
        request.setCustomerEmail("email-invalide");

        mockMvc.perform(post("/api/events/" + event.getId() + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.customerEmail").value("Invalid email format"));
    }
}

