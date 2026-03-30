package com.example.backend.integration;

import com.example.backend.model.CategoryEntity;
import com.example.backend.model.CategoryRepository;
import com.example.backend.model.ConfirmationRepository;
import com.example.backend.model.EventEntity;
import com.example.backend.model.EventRepository;
import com.example.backend.model.EventStatus;
import com.example.backend.model.ReservationRepository;
import com.example.backend.model.UserEntity;
import com.example.backend.model.UserRepository;
import com.example.backend.model.UserRole;
import com.example.backend.model.VenueEntity;
import com.example.backend.model.VenueRepository;
import com.example.backend.service.PasswordHasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.TimeZone;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected EventRepository eventRepository;

    @Autowired
    protected VenueRepository venueRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    @Autowired
    protected ConfirmationRepository confirmationRepository;

    @Autowired
    protected PasswordHasher passwordHasher;

    @BeforeEach
    void clearDatabase() {
        confirmationRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected UserEntity createUser(String email, String phone, UserRole role) {
        UserEntity user = new UserEntity();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPasswordHash(passwordHasher.hash("Password1!"));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    protected CategoryEntity createCategory(String name) {
        CategoryEntity cat = new CategoryEntity();
        cat.setCategoryName(name);
        return categoryRepository.save(cat);
    }

    protected VenueEntity createVenue(String name, String city) {
        VenueEntity venue = new VenueEntity();
        venue.setVenueName(name);
        venue.setCity(city);
        venue.setAddress("123 Test Street");
        venue.setCapacity(500);
        return venueRepository.save(venue);
    }

    protected EventEntity createEvent(String title, VenueEntity venue, CategoryEntity category,
                                       int tickets, EventStatus status) {
        EventEntity event = new EventEntity();
        event.setTitle(title);
        event.setDescription("Test event description");
        event.setEventDate(LocalDateTime.now().plusDays(30));
        event.setAvailableTickets(tickets);
        event.setPrice(new BigDecimal("25.00"));
        event.setStatus(status);
        event.setVenue(venue);
        event.setCategory(category);
        event.setCreatedAt(LocalDateTime.now());
        event.setCreatedBy(1);
        return eventRepository.save(event);
    }
}
