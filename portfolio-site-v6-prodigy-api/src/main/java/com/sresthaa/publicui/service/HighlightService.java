package com.sresthaa.publicui.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.publicui.dto.HighlightSummary;
import com.sresthaa.publicui.dto.HighlightUpsertRequest;
import com.sresthaa.publicui.model.Highlight;
import com.sresthaa.publicui.model.HighlightMediaType;
import com.sresthaa.publicui.repository.HighlightRepository;
import com.sresthaa.storage.R2StorageService;

@Service
public class HighlightService {

	private static final Logger log = LoggerFactory.getLogger(HighlightService.class);

	private final HighlightRepository highlightRepository;
	private final R2StorageService r2StorageService;

	public HighlightService(HighlightRepository highlightRepository, R2StorageService r2StorageService) {
		this.highlightRepository = highlightRepository;
		this.r2StorageService = r2StorageService;
	}

	public List<HighlightSummary> listAll() {
		return highlightRepository.findAllByOrderByCreatedAtDesc().stream().map(HighlightSummary::from).toList();
	}

	public HighlightSummary create(HighlightUpsertRequest request) {
		Highlight highlight = new Highlight(null, null, null, null, null, null, null, null);
		applyAndValidate(highlight, request);
		return HighlightSummary.from(highlightRepository.save(highlight));
	}

	public HighlightSummary update(UUID id, HighlightUpsertRequest request) {
		Highlight highlight = highlightRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such highlight"));

		String previousImage = highlight.getImage();
		String previousThumbnailImage = highlight.getThumbnailImage();

		applyAndValidate(highlight, request);
		HighlightSummary summary = HighlightSummary.from(highlightRepository.save(highlight));

		// Replaced (or dropped, e.g. switched to VIDEO) - the old object is now orphaned in R2.
		deleteIfReplaced(previousImage, highlight.getImage());
		deleteIfReplaced(previousThumbnailImage, highlight.getThumbnailImage());

		return summary;
	}

	public void delete(UUID id) {
		Highlight highlight = highlightRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such highlight"));

		highlightRepository.deleteById(id);

		deleteFromStorage(highlight.getImage());
		deleteFromStorage(highlight.getThumbnailImage());
	}

	private void deleteIfReplaced(String previousUrl, String currentUrl) {
		if (previousUrl != null && !previousUrl.equals(currentUrl)) {
			deleteFromStorage(previousUrl);
		}
	}

	// Best-effort: an R2 hiccup here shouldn't fail (or roll back) a content change the admin
	// already confirmed - worst case is a harmless orphaned object, logged for manual cleanup.
	private void deleteFromStorage(String url) {
		if (url == null) {
			return;
		}
		try {
			r2StorageService.delete(url);
		} catch (RuntimeException e) {
			log.warn("Failed to delete R2 object at {}", url, e);
		}
	}

	private void applyAndValidate(Highlight highlight, HighlightUpsertRequest request) {
		String title = request.title() == null ? "" : request.title().trim();
		String tags = request.tags() == null ? "" : request.tags().trim();
		if (title.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
		}
		if (tags.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one tag is required");
		}
		if (request.mediaType() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media type is required");
		}

		if (request.mediaType() == HighlightMediaType.IMAGE) {
			if (blank(request.image()) || blank(request.thumbnailImage())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image and thumbnail are required");
			}
		} else if (blank(request.videoUrl())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video URL is required");
		}

		highlight.setTitle(title);
		highlight.setDescription(blankToNull(request.description()));
		highlight.setCaption(blankToNull(request.caption()));
		highlight.setMediaType(request.mediaType());
		highlight.setTags(tags);

		if (request.mediaType() == HighlightMediaType.IMAGE) {
			highlight.setImage(request.image().trim());
			highlight.setThumbnailImage(request.thumbnailImage().trim());
			highlight.setVideoUrl(null);
		} else {
			highlight.setVideoUrl(request.videoUrl().trim());
			highlight.setImage(null);
			highlight.setThumbnailImage(null);
		}
	}

	private boolean blank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
