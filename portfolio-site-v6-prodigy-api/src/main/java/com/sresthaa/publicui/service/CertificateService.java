package com.sresthaa.publicui.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.publicui.dto.CertificateCreateRequest;
import com.sresthaa.publicui.dto.CertificateSummary;
import com.sresthaa.publicui.model.Certificate;
import com.sresthaa.publicui.repository.CertificateRepository;
import com.sresthaa.storage.R2StorageService;

@Service
public class CertificateService {

	private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

	private final CertificateRepository certificateRepository;
	private final R2StorageService r2StorageService;

	public CertificateService(CertificateRepository certificateRepository, R2StorageService r2StorageService) {
		this.certificateRepository = certificateRepository;
		this.r2StorageService = r2StorageService;
	}

	public List<CertificateSummary> listAll() {
		return certificateRepository.findAllByOrderByCreatedAtDesc().stream().map(CertificateSummary::from).toList();
	}

	public CertificateSummary create(CertificateCreateRequest request) {
		String imageUrl = request.imageUrl() == null ? "" : request.imageUrl().trim();
		if (imageUrl.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL is required");
		}

		String title = request.title() == null ? "" : request.title().trim();
		Certificate certificate = new Certificate(imageUrl, title.isEmpty() ? null : title);
		return CertificateSummary.from(certificateRepository.save(certificate));
	}

	public void delete(UUID id) {
		Certificate certificate = certificateRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such certificate"));

		certificateRepository.deleteById(id);

		// Best-effort: an R2 hiccup here shouldn't fail the delete the admin already confirmed -
		// worst case is a harmless orphaned object, logged for manual cleanup.
		try {
			r2StorageService.delete(certificate.getImageUrl());
		} catch (RuntimeException e) {
			log.warn("Failed to delete R2 object at {}", certificate.getImageUrl(), e);
		}
	}
}
