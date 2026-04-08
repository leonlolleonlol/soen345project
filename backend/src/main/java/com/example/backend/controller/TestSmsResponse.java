package com.example.backend.controller;

public record TestSmsResponse(
	String sentTo,
	String message
) {
}
