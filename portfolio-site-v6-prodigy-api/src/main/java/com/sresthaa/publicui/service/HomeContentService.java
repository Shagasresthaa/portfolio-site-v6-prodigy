package com.sresthaa.publicui.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sresthaa.publicui.dto.HomeContentPayload;
import com.sresthaa.publicui.model.HomeContent;
import com.sresthaa.publicui.model.TimelineEvent;
import com.sresthaa.publicui.repository.HomeContentRepository;

@Service
public class HomeContentService {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};
	private static final TypeReference<List<TimelineEvent>> TIMELINE_LIST = new TypeReference<>() {
	};

	private final HomeContentRepository homeContentRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public HomeContentService(HomeContentRepository homeContentRepository) {
		this.homeContentRepository = homeContentRepository;
	}

	// No row saved yet - render an empty-but-valid home page rather than an error.
	public HomeContentPayload get() {
		return homeContentRepository.findAll().stream().findFirst()
				.map(this::toPayload)
				.orElseGet(() -> new HomeContentPayload("", List.of(), List.of(), null));
	}

	public HomeContentPayload update(HomeContentPayload payload) {
		if (payload.aboutHook() == null || payload.aboutStory() == null || payload.timeline() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "aboutHook, aboutStory and timeline are required");
		}

		HomeContent content = homeContentRepository.findAll().stream().findFirst()
				.orElseGet(() -> new HomeContent(null, null, null, null));

		content.setAboutHook(payload.aboutHook());
		content.setAboutStoryJson(writeJson(payload.aboutStory()));
		content.setTimelineJson(writeJson(payload.timeline()));
		content.setResumeUrl(payload.resumeUrl());

		return toPayload(homeContentRepository.save(content));
	}

	private HomeContentPayload toPayload(HomeContent content) {
		return new HomeContentPayload(content.getAboutHook(), readJson(content.getAboutStoryJson(), STRING_LIST),
				readJson(content.getTimelineJson(), TIMELINE_LIST), content.getResumeUrl());
	}

	private <T> String writeJson(T value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to serialize home content", e);
		}
	}

	private <T> T readJson(String json, TypeReference<T> type) {
		try {
			return objectMapper.readValue(json, type);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to deserialize home content", e);
		}
	}
}
