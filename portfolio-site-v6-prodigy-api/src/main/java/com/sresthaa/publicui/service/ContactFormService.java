package com.sresthaa.publicui.service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.publicui.dto.ContactMessageSummary;
import com.sresthaa.publicui.model.ContactForm;
import com.sresthaa.publicui.repository.ContactFormRepository;

@Service
public class ContactFormService {

	// Mirrors the client-side pattern in ContactComponent.vue - kept in sync manually, not
	// shared, since one's Java and the other's TS.
	private static final Pattern EMAIL_PATTERN = Pattern
			.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?"
					+ "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)+$");

	private static final int NAME_MAX_LENGTH = 100;
	private static final int EMAIL_MAX_LENGTH = 254;
	private static final int SUBJECT_MAX_LENGTH = 150;
	private static final int MESSAGE_MAX_LENGTH = 5000;

	private final ContactFormRepository contactFormRepository;

	public ContactFormService(ContactFormRepository contactFormRepository) {
		this.contactFormRepository = contactFormRepository;
	}

	public void submit(String name, String email, String subject, String message) {
		String trimmedName = blankToNull(name);
		String trimmedEmail = email == null ? "" : email.trim();
		String trimmedSubject = blankToNull(subject);
		String trimmedMessage = message == null ? "" : message.trim();

		if (trimmedName != null && trimmedName.length() > NAME_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is too long");
		}
		if (trimmedEmail.isEmpty() || trimmedEmail.length() > EMAIL_MAX_LENGTH
				|| !EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email address is required");
		}
		if (trimmedSubject != null && trimmedSubject.length() > SUBJECT_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is too long");
		}
		if (trimmedMessage.isEmpty() || trimmedMessage.length() > MESSAGE_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
		}

		contactFormRepository.save(new ContactForm(trimmedName, trimmedEmail, trimmedSubject, trimmedMessage));
	}

	public List<ContactMessageSummary> listAll() {
		return contactFormRepository.findAllByOrderByCreatedAtDesc().stream().map(ContactMessageSummary::from)
				.toList();
	}

	public void markAsRead(UUID id) {
		ContactForm form = contactFormRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such message"));
		form.setRead(true);
		contactFormRepository.save(form);
	}

	public void delete(UUID id) {
		if (!contactFormRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such message");
		}
		contactFormRepository.deleteById(id);
	}

	private String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
