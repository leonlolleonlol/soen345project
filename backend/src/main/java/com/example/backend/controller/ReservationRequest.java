package com.example.backend.controller;

public record ReservationRequest(
	Integer userId,
	Integer eventId,
	Integer numberOfTickets
) {
}
