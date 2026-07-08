package com.sresthaa.publicui.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.publicui.dto.CertificateCreateRequest;
import com.sresthaa.publicui.dto.CertificateSummary;
import com.sresthaa.publicui.service.CertificateService;

@RestController
public class CertificateController {

	private final CertificateService certificateService;

	public CertificateController(CertificateService certificateService) {
		this.certificateService = certificateService;
	}

	// Every certificate is always public - no draft/published distinction - so this same
	// endpoint serves both the public site's carousel and the admin manager.
	@GetMapping("/api/certificates")
	public List<CertificateSummary> listAll() {
		return certificateService.listAll();
	}

	@PostMapping("/api/admin/certificates")
	public CertificateSummary create(@RequestBody CertificateCreateRequest request) {
		return certificateService.create(request);
	}

	@DeleteMapping("/api/admin/certificates/{id}")
	public void delete(@PathVariable UUID id) {
		certificateService.delete(id);
	}
}
