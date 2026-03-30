package com.example.backend.integration;

import com.example.backend.model.CategoryEntity;
import com.example.backend.model.EventEntity;
import com.example.backend.model.EventStatus;
import com.example.backend.model.UserEntity;
import com.example.backend.model.UserRole;
import com.example.backend.model.VenueEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationIntegrationTest extends BaseIntegrationTest {

    private static final String RESERVATIONS_URL = "/api/reservations";

    @Test
    void createReservation_withValidData_returns201AndDecrementsTickets() throws Exception {
        UserEntity user = createUser("customer@example.com", null, UserRole.CUSTOMER);
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        EventEntity event = createEvent("Rock Night", venue, cat, 10, EventStatus.ACTIVE);

        String body = """
            {
                "userId": %d,
                "eventId": %d,
                "numberOfTickets": 2
            }
            """.formatted(user.getUserId(), event.getEventId());

        mockMvc.perform(post(RESERVATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reservationId").isNumber())
            .andExpect(jsonPath("$.eventTitle").value("Rock Night"))
            .andExpect(jsonPath("$.numberOfTickets").value(2))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        int remaining = eventRepository.findById(event.getEventId()).orElseThrow().getAvailableTickets();
        org.junit.jupiter.api.Assertions.assertEquals(8, remaining);
    }

    @Test
    void createReservation_withInsufficientTickets_returns409() throws Exception {
        UserEntity user = createUser("customer@example.com", null, UserRole.CUSTOMER);
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        EventEntity event = createEvent("Sold Out Show", venue, cat, 1, EventStatus.ACTIVE);

        String body = """
            {
                "userId": %d,
                "eventId": %d,
                "numberOfTickets": 5
            }
            """.formatted(user.getUserId(), event.getEventId());

        mockMvc.perform(post(RESERVATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void createReservation_withNonExistentEvent_returns404() throws Exception {
        UserEntity user = createUser("customer@example.com", null, UserRole.CUSTOMER);

        String body = """
            {
                "userId": %d,
                "eventId": 99999,
                "numberOfTickets": 1
            }
            """.formatted(user.getUserId());

        mockMvc.perform(post(RESERVATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    void getReservationsForUser_returnsUserReservations() throws Exception {
        UserEntity user = createUser("customer@example.com", null, UserRole.CUSTOMER);
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        EventEntity event = createEvent("Rock Night", venue, cat, 10, EventStatus.ACTIVE);

        // Create a reservation via the API
        String body = """
            {
                "userId": %d,
                "eventId": %d,
                "numberOfTickets": 1
            }
            """.formatted(user.getUserId(), event.getEventId());

        mockMvc.perform(post(RESERVATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(get(RESERVATIONS_URL).param("userId", user.getUserId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].eventTitle").value("Rock Night"))
            .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void cancelReservation_returns204AndRestoresTickets() throws Exception {
        UserEntity user = createUser("customer@example.com", null, UserRole.CUSTOMER);
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        EventEntity event = createEvent("Rock Night", venue, cat, 10, EventStatus.ACTIVE);

        // Create reservation
        String createBody = """
            {
                "userId": %d,
                "eventId": %d,
                "numberOfTickets": 3
            }
            """.formatted(user.getUserId(), event.getEventId());

        String response = mockMvc.perform(post(RESERVATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andReturn().getResponse().getContentAsString();

        int reservationId = objectMapper.readTree(response).get("reservationId").asInt();

        // Cancel
        mockMvc.perform(delete(RESERVATIONS_URL + "/" + reservationId))
            .andExpect(status().isNoContent());

        // Tickets restored
        int remaining = eventRepository.findById(event.getEventId()).orElseThrow().getAvailableTickets();
        org.junit.jupiter.api.Assertions.assertEquals(10, remaining);
    }

    @Test
    void cancelReservation_withNonExistentReservation_returns404() throws Exception {
        mockMvc.perform(delete(RESERVATIONS_URL + "/99999"))
            .andExpect(status().isNotFound());
    }
}
