package com.sresthaa.publicui.dto;

import java.time.Instant;

import com.sresthaa.publicui.model.AffiliationType;
import com.sresthaa.publicui.model.CollabMode;
import com.sresthaa.publicui.model.ProjectMediaType;
import com.sresthaa.publicui.model.ProjectStatus;
import com.sresthaa.publicui.model.SourceCodeAvailability;

public record ProjectUpsertRequest(String name, String shortDesc, String longDesc, ProjectStatus statusFlag,
		Instant startDate, Instant endDate, CollabMode collabMode, String affiliation, AffiliationType affiliationType,
		SourceCodeAvailability sourceCodeAvailability, String techStacks, String projectUrl, String liveUrl,
		ProjectMediaType mediaType, String image, String videoUrl) {
}
