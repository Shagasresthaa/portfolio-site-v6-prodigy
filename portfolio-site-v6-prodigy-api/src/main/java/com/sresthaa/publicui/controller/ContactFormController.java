package com.sresthaa.publicui.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.publicui.dto.ContactMessageSummary;
import com.sresthaa.publicui.dto.ContactSubmissionRequest;
import com.sresthaa.publicui.service.ContactFormService;

@RestController
public class ContactFormController {

	private final ContactFormService contactFormService;

	public ContactFormController(ContactFormService contactFormService) {
		this.contactFormService = contactFormService;
	}

	@PostMapping("/api/contact")
	public void submit(@RequestBody ContactSubmissionRequest request) {
		contactFormService.submit(request.name(), request.email(), request.subject(), request.message());
	}

	@GetMapping("/api/admin/contact")
	public List<ContactMessageSummary> listMessages() {
		return contactFormService.listAll();
	}

	@PostMapping("/api/admin/contact/{id}/read")
	public void markAsRead(@PathVariable UUID id) {
		contactFormService.markAsRead(id);
	}

	@DeleteMapping("/api/admin/contact/{id}")
	public void deleteMessage(@PathVariable UUID id) {
		contactFormService.delete(id);
	}
}
