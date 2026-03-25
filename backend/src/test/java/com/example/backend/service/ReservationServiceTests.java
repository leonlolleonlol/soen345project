package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.controller.ReservationRequest;
import com.example.backend.controller.ReservationResponse;
import com.example.backend.model.EventEntity;
import com.example.backend.model.EventRepository;
import com.example.backend.model.EventStatus;
import com.example.backend.model.ReservationEntity;
import com.example.backend.model.ReservationRepository;
import com.example.backend.model.ReservationStatus;
import com.example.backend.model.UserEntity;
import com.example.backend.model.UserRepository;
import com.example.backend.model.UserRole;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private ReservationService reservationService;

	@Captor
	private ArgumentCaptor<ReservationEntity> reservationCaptor;

	@Captor
	private ArgumentCaptor<EventEntity> eventCaptor;

	private EventEntity makeEvent(int id, int availableTickets) {
		EventEntity event = new EventEntity();
		event.setEventId(id);
		event.setTitle("Test Concert");
		event.setAvailableTickets(availableTickets);
		event.setPrice(new BigDecimal("50.00"));
		event.setStatus(EventStatus.ACTIVE);
		event.setEventDate(LocalDateTime.of(2026, 6, 1, 20, 0));
		return event;
	}

	private UserEntity makeUser(int id) {
		UserEntity user = new UserEntity();
		user.setUserId(id);
		user.setFirstName("Jane");
		user.setLastName("Doe");
		user.setEmail("jane@example.com");
		user.setRole(UserRole.CUSTOMER);
		user.setCreatedAt(LocalDateTime.now());
		return user;
	}

	@Test
	void createReservationReturnsConfirmedReservation() {
		EventEntity event = makeEvent(1, 100);
		UserEntity user = makeUser(2);

		when(eventRepository.findById(1)).thenReturn(Optional.of(event));
		when(userRepository.findById(2)).thenReturn(Optional.of(user));
		when(reservationRepository.save(any(ReservationEntity.class))).thenAnswer(inv -> {
			ReservationEntity r = inv.getArgument(0, ReservationEntity.class);
			r.setReservationId(10);
			return r;
		});

		ReservationResponse response = reservationService.createReservation(
			new ReservationRequest(2, 1, 3)
		);

		assertEquals(10, response.reservationId());
		assertEquals("Test Concert", response.eventTitle());
		assertEquals(3, response.numberOfTickets());
		assertEquals(new BigDecimal("150.00"), response.totalPrice());
		assertEquals("CONFIRMED", response.status());
	}

	@Test
	void createReservationDecrementsAvailableTickets() {
		EventEntity event = makeEvent(1, 100);
		UserEntity user = makeUser(2);

		when(eventRepository.findById(1)).thenReturn(Optional.of(event));
		when(userRepository.findById(2)).thenReturn(Optional.of(user));
		when(reservationRepository.save(any(ReservationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		reservationService.createReservation(new ReservationRequest(2, 1, 4));

		verify(eventRepository).save(eventCaptor.capture());
		assertEquals(96, eventCaptor.getValue().getAvailableTickets());
	}

	@Test
	void createReservationThrowsNotFoundWhenEventMissing() {
		when(eventRepository.findById(99)).thenReturn(Optional.empty());

		ResponseStatusException ex = assertThrows(
			ResponseStatusException.class,
			() -> reservationService.createReservation(new ReservationRequest(2, 99, 1))
		);

		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void createReservationThrowsConflictWhenNotEnoughTickets() {
		EventEntity event = makeEvent(1, 2);
		when(eventRepository.findById(1)).thenReturn(Optional.of(event));

		ResponseStatusException ex = assertThrows(
			ResponseStatusException.class,
			() -> reservationService.createReservation(new ReservationRequest(2, 1, 5))
		);

		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void createReservationThrowsNotFoundWhenUserMissing() {
		EventEntity event = makeEvent(1, 100);
		when(eventRepository.findById(1)).thenReturn(Optional.of(event));
		when(userRepository.findById(99)).thenReturn(Optional.empty());

		ResponseStatusException ex = assertThrows(
			ResponseStatusException.class,
			() -> reservationService.createReservation(new ReservationRequest(99, 1, 1))
		);

		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void createReservationSavesReservationWithCorrectStatus() {
		EventEntity event = makeEvent(1, 100);
		UserEntity user = makeUser(2);

		when(eventRepository.findById(1)).thenReturn(Optional.of(event));
		when(userRepository.findById(2)).thenReturn(Optional.of(user));
		when(reservationRepository.save(any(ReservationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		reservationService.createReservation(new ReservationRequest(2, 1, 2));

		verify(reservationRepository).save(reservationCaptor.capture());
		ReservationEntity saved = reservationCaptor.getValue();
		assertEquals(ReservationStatus.CONFIRMED, saved.getStatus());
		assertEquals(2, saved.getNumberOfTickets());
		assertEquals(new BigDecimal("100.00"), saved.getTotalPrice());
	}
}
