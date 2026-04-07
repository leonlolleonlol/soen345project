package com.example.backend.integration;

import com.example.backend.model.CategoryEntity;
import com.example.backend.model.EventStatus;
import com.example.backend.model.VenueEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventIntegrationTest extends BaseIntegrationTest {

    private static final String EVENTS_URL = "/api/events";

    @Test
    void getActiveEvents_returnsPagedResponse() throws Exception {
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        createEvent("Rock Night", venue, cat, 100, EventStatus.ACTIVE);
        createEvent("Jazz Evening", venue, cat, 50, EventStatus.ACTIVE);

        mockMvc.perform(get(EVENTS_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(2)))
            .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void getActiveEvents_excludesCancelledEvents() throws Exception {
        CategoryEntity cat = createCategory("Concert");
        VenueEntity venue = createVenue("Bell Centre", "Montreal");
        createEvent("Active Show", venue, cat, 100, EventStatus.ACTIVE);
        createEvent("Cancelled Show", venue, cat, 100, EventStatus.CANCELLED);

        mockMvc.perform(get(EVENTS_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(1)))
            .andExpect(jsonPath("$.events[0].title").value("Active Show"));
    }

    @Test
    void getActiveEvents_filterByCity_returnsMatchingEventsOnly() throws Exception {
        CategoryEntity cat = createCategory("Sports");
        VenueEntity montreal = createVenue("Olympic Stadium", "Montreal");
        VenueEntity toronto = createVenue("Rogers Centre", "Toronto");
        createEvent("Montreal Game", montreal, cat, 200, EventStatus.ACTIVE);
        createEvent("Toronto Game", toronto, cat, 200, EventStatus.ACTIVE);

        mockMvc.perform(get(EVENTS_URL).param("city", "Montreal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(1)))
            .andExpect(jsonPath("$.events[0].venueCity").value("Montreal"));
    }

    @Test
    void getActiveEvents_filterByCategory_returnsMatchingEventsOnly() throws Exception {
        CategoryEntity concert = createCategory("Concert");
        CategoryEntity sports = createCategory("Sports");
        VenueEntity venue = createVenue("Arena", "Montreal");
        createEvent("Rock Show", venue, concert, 100, EventStatus.ACTIVE);
        createEvent("Hockey Game", venue, sports, 100, EventStatus.ACTIVE);

        mockMvc.perform(get(EVENTS_URL).param("category", "Concert"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(1)))
            .andExpect(jsonPath("$.events[0].categoryName").value("Concert"));
    }

    @Test
    void getActiveEvents_filterByFromDate_returnsOnlyFutureEvents() throws Exception {
        CategoryEntity cat = createCategory("Theatre");
        VenueEntity venue = createVenue("Theatre Royal", "Montreal");
        createEvent("Past Show", venue, cat, 100, EventStatus.ACTIVE);

        // Shift one event far into the future so fromDate filter excludes the near-future event
        var futureEvent = createEvent("Future Show", venue, cat, 100, EventStatus.ACTIVE);
        futureEvent.setEventDate(futureEvent.getEventDate().plusYears(1));
        eventRepository.save(futureEvent);

        String filterDate = LocalDate.now().plusMonths(6).format(DateTimeFormatter.ISO_DATE);

        mockMvc.perform(get(EVENTS_URL).param("fromDate", filterDate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(1)))
            .andExpect(jsonPath("$.events[0].title").value("Future Show"));
    }

    @Test
    void getActiveEvents_filterByKeyword_returnsMatchingTitle() throws Exception {
        CategoryEntity cat = createCategory("Comedy");
        VenueEntity venue = createVenue("Comedy Club", "Montreal");
        createEvent("Stand-Up Night", venue, cat, 80, EventStatus.ACTIVE);
        createEvent("Jazz Evening", venue, cat, 80, EventStatus.ACTIVE);

        mockMvc.perform(get(EVENTS_URL).param("keyword", "stand"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(1)))
            .andExpect(jsonPath("$.events[0].title").value("Stand-Up Night"));
    }

    @Test
    void getActiveEvents_returnsEmptyListWhenNoEvents() throws Exception {
        mockMvc.perform(get(EVENTS_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", hasSize(0)))
            .andExpect(jsonPath("$.hasMore").value(false));
    }
}
