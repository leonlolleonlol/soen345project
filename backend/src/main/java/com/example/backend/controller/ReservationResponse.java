package com.example.backend.controller;

import java.math.BigDecimal;

public record ReservationResponse(
	Integer reservationId,
	String eventTitle,
	Integer numberOfTickets,
	BigDecimal totalPrice,
	String status
) {
}
