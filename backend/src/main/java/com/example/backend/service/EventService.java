package com.example.backend.service;

import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.controller.EventResponse;
import com.example.backend.controller.PagedEventResponse;
import com.example.backend.model.EventRepository;
import com.example.backend.model.EventStatus;

@Service
public class EventService {

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final int PAGE_SIZE = 10;

	private final EventRepository eventRepository;

	public EventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@Transactional(readOnly = true)
	public PagedEventResponse getActiveEvents(int page) {
		Page<EventResponse> result = eventRepository
			.findByStatusOrderByEventDateAsc(EventStatus.ACTIVE, PageRequest.of(page, PAGE_SIZE))
			.map(e -> new EventResponse(
				e.getEventId(),
				e.getTitle(),
				e.getDescription(),
				e.getEventDate().format(ISO_FORMATTER),
				e.getAvailableTickets(),
				e.getPrice(),
				e.getStatus().name(),
				e.getVenue().getVenueName(),
				e.getVenue().getCity(),
				e.getCategory().getCategoryName()
			));
		return new PagedEventResponse(result.getContent(), !result.isLast());
	}
}
