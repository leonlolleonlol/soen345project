package com.example.backend.integration;

import com.example.backend.model.CategoryEntity;
import com.example.backend.model.EventStatus;
import com.example.backend.model.UserRole;
import com.example.backend.model.VenueEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminEventIntegrationTest extends BaseIntegrationTest {

    private static final String ADMIN_EVENTS_URL = "/api/admin/events";

    private String eventJson(String title, String categoryName, String venueName) {
        return """
            {
                "title": "%s",
                "description": "A great event",
                "eventDate": "2027-06-15T20:00:00",
                "availableTickets": 200,
                "price": 49.99,
                "venueName": "%s",
                "venueCity": "Montreal",
                "venueAddress": "1 Test Ave",
                "venueCapacity": 5000,
                "categoryName": "%s",
                "createdBy": 1
            }
            """.formatted(title, venueName, categoryName);
    }

    @Test
    void createEvent_withValidData_returns201AndEventFields() throws Exception {
        createCategory("Concert");
        createUser("admin@example.com", null, UserRole.ADMIN);

        mockMvc.perform(post(ADMIN_EVENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Rock Night", "Concert", "Bell Centre")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Rock Night"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.categoryName").value("Concert"))
            .andExpect(jsonPath("$.eventId").isNumber());
    }

    @Test
    void createEvent_reusesExistingVenue() throws Exception {
        createCategory("Concert");
        createVenue("Bell Centre", "Montreal");

        mockMvc.perform(post(ADMIN_EVENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Rock Night 2", "Concert", "Bell Centre")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venueName").value("Bell Centre"));

        // Only one venue should exist
        org.junit.jupiter.api.Assertions.assertEquals(1, venueRepository.count());
    }

    @Test
    void createEvent_withNonExistentCategory_returns400() throws Exception {
        mockMvc.perform(post(ADMIN_EVENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Rock Night", "NonExistentCategory", "Bell Centre")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_withValidData_returns200() throws Exception {
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        var event = createEvent("Old Title", venue, cat, 100, EventStatus.ACTIVE);

        mockMvc.perform(put(ADMIN_EVENTS_URL + "/" + event.getEventId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("New Title", "Concert", "Bell Centre")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New Title"))
            .andExpect(jsonPath("$.eventId").value(event.getEventId()));
    }

    @Test
    void updateEvent_withNonExistentId_returns404() throws Exception {
        createCategory("Concert");

        mockMvc.perform(put(ADMIN_EVENTS_URL + "/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Title", "Concert", "Some Venue")))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelEvent_withActiveEvent_returns204() throws Exception {
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        var event = createEvent("Rock Night", venue, cat, 100, EventStatus.ACTIVE);

        mockMvc.perform(patch(ADMIN_EVENTS_URL + "/" + event.getEventId() + "/cancel"))
            .andExpect(status().isNoContent());

        var updated = eventRepository.findById(event.getEventId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(EventStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void cancelEvent_withNonExistentId_returns404() throws Exception {
        mockMvc.perform(patch(ADMIN_EVENTS_URL + "/99999/cancel"))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelEvent_alreadyCancelled_returns409() throws Exception {
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        var event = createEvent("Cancelled Show", venue, cat, 100, EventStatus.CANCELLED);

        mockMvc.perform(patch(ADMIN_EVENTS_URL + "/" + event.getEventId() + "/cancel"))
            .andExpect(status().isConflict());
    }
}
