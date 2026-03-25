package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.backend.controller.PagedEventResponse;
import com.example.backend.model.CategoryEntity;
import com.example.backend.model.EventEntity;
import com.example.backend.model.EventRepository;
import com.example.backend.model.EventStatus;
import com.example.backend.model.VenueEntity;

@ExtendWith(MockitoExtension.class)
class EventServiceTests {

	@Mock
	private EventRepository eventRepository;

	@InjectMocks
	private EventService eventService;

	private EventEntity makeEvent(int id, String title) {
		VenueEntity venue = new VenueEntity();
		venue.setVenueName("Bell Centre");
		venue.setCity("Montreal");

		CategoryEntity category = new CategoryEntity();
		category.setCategoryName("Concert");

		EventEntity event = new EventEntity();
		event.setEventId(id);
		event.setTitle(title);
		event.setDescription("A great show");
		event.setEventDate(LocalDateTime.of(2026, 6, 1, 20, 0));
		event.setAvailableTickets(100);
		event.setPrice(new BigDecimal("99.99"));
		event.setStatus(EventStatus.ACTIVE);
		event.setVenue(venue);
		event.setCategory(category);
		return event;
	}

	@Test
	void getActiveEventsReturnsEventsOnFirstPage() {
		List<EventEntity> events = List.of(makeEvent(1, "Show A"), makeEvent(2, "Show B"));
		Page<EventEntity> page = new PageImpl<>(events);
		when(eventRepository.findByStatusOrderByEventDateAsc(eq(EventStatus.ACTIVE), any(Pageable.class)))
			.thenReturn(page);

		PagedEventResponse response = eventService.getActiveEvents(0);

		assertEquals(2, response.events().size());
		assertEquals("Show A", response.events().get(0).title());
		assertEquals("Show B", response.events().get(1).title());
	}

	@Test
	void getActiveEventsMapsFieldsCorrectly() {
		Page<EventEntity> page = new PageImpl<>(List.of(makeEvent(5, "Jazz Night")));
		when(eventRepository.findByStatusOrderByEventDateAsc(eq(EventStatus.ACTIVE), any(Pageable.class)))
			.thenReturn(page);

		PagedEventResponse response = eventService.getActiveEvents(0);

		var event = response.events().get(0);
		assertEquals(5, event.eventId());
		assertEquals("Jazz Night", event.title());
		assertEquals("Bell Centre", event.venueName());
		assertEquals("Montreal", event.venueCity());
		assertEquals("Concert", event.categoryName());
		assertEquals(new BigDecimal("99.99"), event.price());
		assertEquals("ACTIVE", event.status());
	}

	@Test
	void getActiveEventsHasMoreTrueWhenNotLastPage() {
		List<EventEntity> content = List.of(makeEvent(1, "Show A"));
		// totalElements=20, pageSize=10 → 2 pages → first page is not last
		Page<EventEntity> page = new PageImpl<>(content, org.springframework.data.domain.PageRequest.of(0, 10), 20);
		when(eventRepository.findByStatusOrderByEventDateAsc(eq(EventStatus.ACTIVE), any(Pageable.class)))
			.thenReturn(page);

		PagedEventResponse response = eventService.getActiveEvents(0);

		assertTrue(response.hasMore());
	}

	@Test
	void getActiveEventsHasMoreFalseOnLastPage() {
		List<EventEntity> content = List.of(makeEvent(1, "Show A"));
		Page<EventEntity> page = new PageImpl<>(content, org.springframework.data.domain.PageRequest.of(0, 10), 1);
		when(eventRepository.findByStatusOrderByEventDateAsc(eq(EventStatus.ACTIVE), any(Pageable.class)))
			.thenReturn(page);

		PagedEventResponse response = eventService.getActiveEvents(0);

		assertFalse(response.hasMore());
	}

	@Test
	void getActiveEventsReturnsEmptyListWhenNoEvents() {
		when(eventRepository.findByStatusOrderByEventDateAsc(eq(EventStatus.ACTIVE), any(Pageable.class)))
			.thenReturn(Page.empty());

		PagedEventResponse response = eventService.getActiveEvents(0);

		assertTrue(response.events().isEmpty());
		assertFalse(response.hasMore());
	}
}
