package com.example.backend.system;

import com.example.backend.model.CategoryEntity;
import com.example.backend.model.CategoryRepository;
import com.example.backend.model.ConfirmationRepository;
import com.example.backend.model.EventRepository;
import com.example.backend.model.EventStatus;
import com.example.backend.model.ReservationRepository;
import com.example.backend.model.UserEntity;
import com.example.backend.model.UserRepository;
import com.example.backend.model.UserRole;
import com.example.backend.model.VenueRepository;
import com.example.backend.service.PasswordHasher;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * System test: verifies the complete admin journey through the application.
 * Create Event → Verify in Public Listing → Update Event → Cancel Event → Verify Removed from Listing
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class AdminJourneySystemTest {

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

    @Autowired
    private PasswordHasher passwordHasher;

    @BeforeEach
    void clearDatabase() {
        confirmationRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private UserEntity seedAdminUser() {
        UserEntity admin = new UserEntity();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@systemtest.com");
        admin.setPhoneNumber(null);
        admin.setPasswordHash(passwordHasher.hash("AdminPass1!"));
        admin.setRole(UserRole.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        return userRepository.save(admin);
    }

    private CategoryEntity seedCategory(String name) {
        CategoryEntity cat = new CategoryEntity();
        cat.setCategoryName(name);
        return categoryRepository.save(cat);
    }

    private String createEventJson(String title, String categoryName) {
        return """
            {
                "title": "%s",
                "description": "A fantastic event",
                "eventDate": "2027-08-20T19:00:00",
                "availableTickets": 500,
                "price": 59.99,
                "venueName": "Olympic Stadium",
                "venueCity": "Montreal",
                "venueAddress": "4141 Avenue Pierre-De Coubertin",
                "venueCapacity": 56040,
                "categoryName": "%s",
                "createdBy": 1
            }
            """.formatted(title, categoryName);
    }

    @Test
    void completeAdminJourney() throws Exception {
        // Setup: seed admin user and category
        UserEntity admin = seedAdminUser();
        seedCategory("Sports");

        // --- Step 1: Create a new event ---
        MvcResult createResult = mockMvc.perform(post("/api/admin/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createEventJson("Championship Final", "Sports")))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode createdEvent = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int eventId = createdEvent.get("eventId").asInt();
        assertEquals("Championship Final", createdEvent.get("title").asText());
        assertEquals("ACTIVE", createdEvent.get("status").asText());
        assertEquals("Sports", createdEvent.get("categoryName").asText());

        // --- Step 2: Verify event appears in the public listing ---
        MvcResult listingResult = mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode listing = objectMapper.readTree(listingResult.getResponse().getContentAsString());
        assertEquals(1, listing.get("events").size());
        assertEquals("Championship Final", listing.get("events").get(0).get("title").asText());

        // --- Step 3: Update the event title ---
        MvcResult updateResult = mockMvc.perform(put("/api/admin/events/" + eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createEventJson("Championship Grand Final", "Sports")))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode updatedEvent = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertEquals("Championship Grand Final", updatedEvent.get("title").asText());
        assertEquals(eventId, updatedEvent.get("eventId").asInt());

        // Verify updated title reflects in public listing
        MvcResult updatedListingResult = mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode updatedListing = objectMapper.readTree(updatedListingResult.getResponse().getContentAsString());
        assertEquals("Championship Grand Final", updatedListing.get("events").get(0).get("title").asText());

        // --- Step 4: Cancel the event ---
        mockMvc.perform(patch("/api/admin/events/" + eventId + "/cancel"))
            .andExpect(status().isNoContent());

        // --- Step 5: Verify event is removed from the public listing ---
        MvcResult afterCancelResult = mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode afterCancel = objectMapper.readTree(afterCancelResult.getResponse().getContentAsString());
        assertEquals(0, afterCancel.get("events").size());

        // Verify CANCELLED status in database
        assertEquals(EventStatus.CANCELLED,
            eventRepository.findById(eventId).orElseThrow().getStatus());
    }
}
