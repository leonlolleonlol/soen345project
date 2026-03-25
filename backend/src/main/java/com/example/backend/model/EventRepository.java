package com.example.backend.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Integer> {

	Page<EventEntity> findByStatusOrderByEventDateAsc(EventStatus status, Pageable pageable);
}
