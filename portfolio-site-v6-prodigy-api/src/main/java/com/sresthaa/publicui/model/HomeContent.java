package com.sresthaa.publicui.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Singleton content - always exactly one row, read/replaced as a whole (see
// HomeContentService). aboutStoryJson/timelineJson hold JSON-encoded arrays, not normalized
// child tables - nothing ever queries into their structure, only the whole blob.
@Entity
@Table(name = "home_content")
public class HomeContent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "about_hook", nullable = false, columnDefinition = "TEXT")
	private String aboutHook;

	@Column(name = "about_story_json", nullable = false, columnDefinition = "TEXT")
	private String aboutStoryJson;

	@Column(name = "timeline_json", nullable = false, columnDefinition = "TEXT")
	private String timelineJson;

	protected HomeContent() {
	}

	public HomeContent(String aboutHook, String aboutStoryJson, String timelineJson) {
		this.aboutHook = aboutHook;
		this.aboutStoryJson = aboutStoryJson;
		this.timelineJson = timelineJson;
	}

	public UUID getId() {
		return id;
	}

	public String getAboutHook() {
		return aboutHook;
	}

	public void setAboutHook(String aboutHook) {
		this.aboutHook = aboutHook;
	}

	public String getAboutStoryJson() {
		return aboutStoryJson;
	}

	public void setAboutStoryJson(String aboutStoryJson) {
		this.aboutStoryJson = aboutStoryJson;
	}

	public String getTimelineJson() {
		return timelineJson;
	}

	public void setTimelineJson(String timelineJson) {
		this.timelineJson = timelineJson;
	}
}
