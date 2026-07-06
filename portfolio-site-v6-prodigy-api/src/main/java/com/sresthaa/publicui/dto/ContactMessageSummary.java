package com.sresthaa.publicui.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.publicui.model.ContactForm;

public record ContactMessageSummary(UUID id, String name, String email, String subject, String message,
		boolean read, Instant createdAt) {

	public static ContactMessageSummary from(ContactForm form) {
		return new ContactMessageSummary(form.getId(), form.getName(), form.getEmail(), form.getSubject(),
				form.getMessage(), form.isRead(), form.getCreatedAt());
	}
}
