package com.example.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.controller.ReservationRequest;
import com.example.backend.controller.ReservationResponse;
import com.example.backend.model.EventRepository;
import com.example.backend.model.ReservationEntity;
import com.example.backend.model.ReservationRepository;
import com.example.backend.model.ReservationStatus;
import com.example.backend.model.UserRepository;

@Service
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	public ReservationService(ReservationRepository reservationRepository,
							  EventRepository eventRepository,
							  UserRepository userRepository) {
		this.reservationRepository = reservationRepository;
		this.eventRepository = eventRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public ReservationResponse createReservation(ReservationRequest request) {
		var event = eventRepository.findById(request.eventId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

		if (event.getAvailableTickets() < request.numberOfTickets()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough tickets available");
		}

		var user = userRepository.findById(request.userId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		event.setAvailableTickets(event.getAvailableTickets() - request.numberOfTickets());
		eventRepository.save(event);

		var reservation = new ReservationEntity();
		reservation.setUser(user);
		reservation.setEvent(event);
		reservation.setNumberOfTickets(request.numberOfTickets());
		reservation.setReservationDate(LocalDateTime.now());
		reservation.setStatus(ReservationStatus.CONFIRMED);
		reservation.setTotalPrice(event.getPrice().multiply(BigDecimal.valueOf(request.numberOfTickets())));

		var saved = reservationRepository.save(reservation);

		return new ReservationResponse(
			saved.getReservationId(),
			event.getTitle(),
			saved.getNumberOfTickets(),
			saved.getTotalPrice(),
			saved.getStatus().name()
		);
	}
}
