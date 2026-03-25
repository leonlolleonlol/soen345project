package com.example.backend.controller;

import java.util.List;

public record PagedEventResponse(
	List<EventResponse> events,
	boolean hasMore
) {
}
