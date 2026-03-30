package com.example.backend.system;

import com.example.backend.model.CategoryEntity;
import com.example.backend.model.CategoryRepository;
import com.example.backend.model.ConfirmationRepository;
import com.example.backend.model.EventEntity;
import com.example.backend.model.EventRepository;
import com.example.backend.model.EventStatus;
import com.example.backend.model.ReservationRepository;
import com.example.backend.model.UserRepository;
import com.example.backend.model.VenueEntity;
import com.example.backend.model.VenueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * System test: verifies the complete customer journey through the application.
 * Register → Login → Browse Events → Make Reservation → View Reservations → Cancel Reservation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class CustomerJourneySystemTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ConfirmationRepository confirmationRepository;

    @BeforeEach
    void clearDatabase() {
        confirmationRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private EventEntity seedEvent() {
        CategoryEntity cat = new CategoryEntity();
        cat.setCategoryName("Concert");
        categoryRepository.save(cat);

        VenueEntity venue = new VenueEntity();
        venue.setVenueName("Bell Centre");
        venue.setCity("Montreal");
        venue.setAddress("1909 Avenue des Canadiens-de-Montréal");
        venue.setCapacity(21302);
        venueRepository.save(venue);

        EventEntity event = new EventEntity();
        event.setTitle("Rock Night");
        event.setDescription("An amazing rock concert");
        event.setEventDate(LocalDateTime.now().plusDays(30));
        event.setAvailableTickets(50);
        event.setPrice(new BigDecimal("39.99"));
        event.setStatus(EventStatus.ACTIVE);
        event.setVenue(venue);
        event.setCategory(cat);
        event.setCreatedAt(LocalDateTime.now());
        event.setCreatedBy(1);
        return eventRepository.save(event);
    }

    @Test
    void completeCustomerJourney() throws Exception {
        // --- Step 1: Register a new customer ---
        String registerBody = """
            {
                "firstName": "Alice",
                "lastName": "Tester",
                "email": "alice@systemtest.com",
                "phoneNumber": null,
                "password": "SecurePass1!"
            }
            """;

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        int userId = registerJson.get("userId").asInt();
        assertEquals("alice@systemtest.com", registerJson.get("email").asText());
        assertEquals("CUSTOMER", registerJson.get("role").asText());

        // --- Step 2: Login with registered credentials ---
        String loginBody = """
            {
                "email": "alice@systemtest.com",
                "password": "SecurePass1!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        assertEquals(userId, loginJson.get("userId").asInt());
        assertEquals("CUSTOMER", loginJson.get("role").asText());

        // --- Step 3: Browse available events ---
        EventEntity seededEvent = seedEvent();

        MvcResult eventsResult = mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode eventsJson = objectMapper.readTree(eventsResult.getResponse().getContentAsString());
        assertEquals(1, eventsJson.get("events").size());
        assertEquals("Rock Night", eventsJson.get("events").get(0).get("title").asText());
        assertNotNull(eventsJson.get("hasMore"));

        // --- Step 4: Make a reservation ---
        String reservationBody = """
            {
                "userId": %d,
                "eventId": %d,
                "numberOfTickets": 2
            }
            """.formatted(userId, seededEvent.getEventId());

        MvcResult reservationResult = mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationBody))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode reservationJson = objectMapper.readTree(reservationResult.getResponse().getContentAsString());
        int reservationId = reservationJson.get("reservationId").asInt();
        assertEquals("Rock Night", reservationJson.get("eventTitle").asText());
        assertEquals(2, reservationJson.get("numberOfTickets").asInt());
        assertEquals("CONFIRMED", reservationJson.get("status").asText());

        // Tickets decremented
        int remaining = eventRepository.findById(seededEvent.getEventId()).orElseThrow().getAvailableTickets();
        assertEquals(48, remaining);

        // --- Step 5: View my reservations ---
        MvcResult myReservationsResult = mockMvc.perform(get("/api/reservations")
                .param("userId", String.valueOf(userId)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode myReservationsJson = objectMapper.readTree(myReservationsResult.getResponse().getContentAsString());
        assertEquals(1, myReservationsJson.size());
        assertEquals("Rock Night", myReservationsJson.get(0).get("eventTitle").asText());
        assertEquals("CONFIRMED", myReservationsJson.get(0).get("status").asText());

        // --- Step 6: Cancel the reservation ---
        mockMvc.perform(delete("/api/reservations/" + reservationId))
            .andExpect(status().isNoContent());

        // Tickets restored
        int restoredTickets = eventRepository.findById(seededEvent.getEventId()).orElseThrow().getAvailableTickets();
        assertEquals(50, restoredTickets);

        // --- Step 7: Verify reservation is now cancelled ---
        MvcResult afterCancelResult = mockMvc.perform(get("/api/reservations")
                .param("userId", String.valueOf(userId)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode afterCancelJson = objectMapper.readTree(afterCancelResult.getResponse().getContentAsString());
        assertEquals("CANCELLED", afterCancelJson.get(0).get("status").asText());
    }
}
