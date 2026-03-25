package com.example.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class ReservationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Integer reservationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private EventEntity event;

	@Column(name = "number_of_tickets", nullable = false)
	private Integer numberOfTickets;

	@Column(name = "reservation_date")
	private LocalDateTime reservationDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private ReservationStatus status;

	@Column(name = "total_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal totalPrice;

	public Integer getReservationId() { return reservationId; }
	public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }

	public UserEntity getUser() { return user; }
	public void setUser(UserEntity user) { this.user = user; }

	public EventEntity getEvent() { return event; }
	public void setEvent(EventEntity event) { this.event = event; }

	public Integer getNumberOfTickets() { return numberOfTickets; }
	public void setNumberOfTickets(Integer numberOfTickets) { this.numberOfTickets = numberOfTickets; }

	public LocalDateTime getReservationDate() { return reservationDate; }
	public void setReservationDate(LocalDateTime reservationDate) { this.reservationDate = reservationDate; }

	public ReservationStatus getStatus() { return status; }
	public void setStatus(ReservationStatus status) { this.status = status; }

	public BigDecimal getTotalPrice() { return totalPrice; }
	public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
