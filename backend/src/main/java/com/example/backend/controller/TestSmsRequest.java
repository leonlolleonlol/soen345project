package com.example.backend.controller;

public record TestSmsRequest(
	String phoneNumber,
	String message
) {
}
