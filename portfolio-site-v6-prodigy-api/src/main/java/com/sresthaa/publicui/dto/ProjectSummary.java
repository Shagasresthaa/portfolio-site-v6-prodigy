package com.sresthaa.publicui.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.publicui.model.AffiliationType;
import com.sresthaa.publicui.model.CollabMode;
import com.sresthaa.publicui.model.Project;
import com.sresthaa.publicui.model.ProjectMediaType;
import com.sresthaa.publicui.model.ProjectStatus;
import com.sresthaa.publicui.model.SourceCodeAvailability;

public record ProjectSummary(UUID id, String name, String shortDesc, String longDesc, ProjectStatus statusFlag,
		Instant startDate, Instant endDate, CollabMode collabMode, String affiliation, AffiliationType affiliationType,
		SourceCodeAvailability sourceCodeAvailability, String techStacks, String projectUrl, String liveUrl,
		ProjectMediaType mediaType, String image, String videoUrl) {

	public static ProjectSummary from(Project project) {
		return new ProjectSummary(project.getId(), project.getName(), project.getShortDesc(), project.getLongDesc(),
				project.getStatusFlag(), project.getStartDate(), project.getEndDate(), project.getCollabMode(),
				project.getAffiliation(), project.getAffiliationType(), project.getSourceCodeAvailability(),
				project.getTechStacks(), project.getProjectUrl(), project.getLiveUrl(), project.getMediaType(),
				project.getImage(), project.getVideoUrl());
	}
}
