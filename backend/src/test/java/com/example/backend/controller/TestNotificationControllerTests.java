package com.example.backend.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.notification.SmsProvider;

@WebMvcTest(TestNotificationController.class)
@TestPropertySource(properties = {
	"app.test-sms.enabled=true",
	"app.test-sms.api-key=test-secret"
})
class TestNotificationControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SmsProvider smsProvider;

	@Test
	void sendTestSmsReturns200AndNormalizesCanadianNumber() throws Exception {
		mockMvc.perform(post("/api/test/sms")
				.header("X-Test-SMS-Key", "test-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"phoneNumber": "514 430 6375", "message": "Hello from test"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sentTo").value("+15144306375"))
			.andExpect(jsonPath("$.message").value("Hello from test"));

		verify(smsProvider).sendSms("+15144306375", "Hello from test");
	}

	@Test
	void sendTestSmsRejectsWrongApiKey() throws Exception {
		mockMvc.perform(post("/api/test/sms")
				.header("X-Test-SMS-Key", "wrong-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"phoneNumber": "514 430 6375"}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void sendTestSmsUsesDefaultMessageWhenBlank() throws Exception {
		mockMvc.perform(post("/api/test/sms")
				.header("X-Test-SMS-Key", "test-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"phoneNumber": "+15144306375", "message": ""}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sentTo").value("+15144306375"))
			.andExpect(jsonPath("$.message").value("SOEN345 test SMS from TicketMonster."));

		verify(smsProvider).sendSms("+15144306375", "SOEN345 test SMS from TicketMonster.");
	}
}
