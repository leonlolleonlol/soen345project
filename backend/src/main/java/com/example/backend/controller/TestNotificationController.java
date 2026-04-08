package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.notification.SmsProvider;

@RestController
@RequestMapping("/api/test")
public class TestNotificationController {

	private static final String DEFAULT_MESSAGE = "SOEN345 test SMS from TicketMonster.";

	private final SmsProvider smsProvider;
	private final boolean testSmsEnabled;
	private final String testSmsApiKey;

	public TestNotificationController(
		SmsProvider smsProvider,
		@Value("${app.test-sms.enabled:false}") boolean testSmsEnabled,
		@Value("${app.test-sms.api-key:}") String testSmsApiKey
	) {
		this.smsProvider = smsProvider;
		this.testSmsEnabled = testSmsEnabled;
		this.testSmsApiKey = testSmsApiKey == null ? "" : testSmsApiKey.trim();
	}

	@PostMapping("/sms")
	@ResponseStatus(HttpStatus.OK)
	public TestSmsResponse sendTestSms(
		@RequestHeader(value = "X-Test-SMS-Key", required = false) String providedApiKey,
		@RequestBody TestSmsRequest request
	) {
		if (!testSmsEnabled) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test SMS endpoint is disabled.");
		}

		if (testSmsApiKey.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Test SMS API key is not configured.");
		}

		if (providedApiKey == null || !testSmsApiKey.equals(providedApiKey.trim())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid test SMS API key.");
		}

		String phoneNumber = normalizePhoneNumber(request.phoneNumber());
		String message = normalizeMessage(request.message());

		smsProvider.sendSms(phoneNumber, message);
		return new TestSmsResponse(phoneNumber, message);
	}

	private static String normalizeMessage(String message) {
		if (message == null || message.trim().isEmpty()) {
			return DEFAULT_MESSAGE;
		}
		return message.trim();
	}

	private static String normalizePhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required.");
		}

		String digits = phoneNumber.replaceAll("[^0-9+]", "");

		if (digits.startsWith("+")) {
			String numericPart = digits.substring(1).replaceAll("[^0-9]", "");
			if (numericPart.length() < 10 || numericPart.length() > 15) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid phone number.");
			}
			return "+" + numericPart;
		}

		String numericPart = digits.replaceAll("[^0-9]", "");
		if (numericPart.length() == 10) {
			return "+1" + numericPart;
		}
		if (numericPart.length() == 11 && numericPart.startsWith("1")) {
			return "+" + numericPart;
		}

		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid phone number.");
	}
}
