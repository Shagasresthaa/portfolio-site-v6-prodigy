package com.sresthaa.publicui.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(name = "short_desc", nullable = false, columnDefinition = "TEXT")
	private String shortDesc;

	@Column(name = "long_desc", columnDefinition = "TEXT")
	private String longDesc;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_flag", nullable = false)
	private ProjectStatus statusFlag;

	@Column(name = "start_date", nullable = false)
	private Instant startDate;

	@Column(name = "end_date")
	private Instant endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "collab_mode", nullable = false)
	private CollabMode collabMode;

	@Column(nullable = false)
	private String affiliation;

	@Enumerated(EnumType.STRING)
	@Column(name = "affiliation_type", nullable = false)
	private AffiliationType affiliationType;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_code_availability", nullable = false)
	private SourceCodeAvailability sourceCodeAvailability;

	// Comma-separated, matching the old site's storage format (see src/types/project.ts on
	// both frontends).
	@Column(name = "tech_stacks", nullable = false)
	private String techStacks;

	@Column(name = "project_url")
	private String projectUrl;

	@Column(name = "live_url")
	private String liveUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type", nullable = false)
	private ProjectMediaType mediaType;

	@Column
	private String image;

	@Column(name = "video_url")
	private String videoUrl;

	protected Project() {
	}

	public Project(String name, String shortDesc, String longDesc, ProjectStatus statusFlag, Instant startDate,
			Instant endDate, CollabMode collabMode, String affiliation, AffiliationType affiliationType,
			SourceCodeAvailability sourceCodeAvailability, String techStacks, String projectUrl, String liveUrl,
			ProjectMediaType mediaType, String image, String videoUrl) {
		this.name = name;
		this.shortDesc = shortDesc;
		this.longDesc = longDesc;
		this.statusFlag = statusFlag;
		this.startDate = startDate;
		this.endDate = endDate;
		this.collabMode = collabMode;
		this.affiliation = affiliation;
		this.affiliationType = affiliationType;
		this.sourceCodeAvailability = sourceCodeAvailability;
		this.techStacks = techStacks;
		this.projectUrl = projectUrl;
		this.liveUrl = liveUrl;
		this.mediaType = mediaType;
		this.image = image;
		this.videoUrl = videoUrl;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortDesc() {
		return shortDesc;
	}

	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}

	public String getLongDesc() {
		return longDesc;
	}

	public void setLongDesc(String longDesc) {
		this.longDesc = longDesc;
	}

	public ProjectStatus getStatusFlag() {
		return statusFlag;
	}

	public void setStatusFlag(ProjectStatus statusFlag) {
		this.statusFlag = statusFlag;
	}

	public Instant getStartDate() {
		return startDate;
	}

	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	public Instant getEndDate() {
		return endDate;
	}

	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

	public CollabMode getCollabMode() {
		return collabMode;
	}

	public void setCollabMode(CollabMode collabMode) {
		this.collabMode = collabMode;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	public AffiliationType getAffiliationType() {
		return affiliationType;
	}

	public void setAffiliationType(AffiliationType affiliationType) {
		this.affiliationType = affiliationType;
	}

	public SourceCodeAvailability getSourceCodeAvailability() {
		return sourceCodeAvailability;
	}

	public void setSourceCodeAvailability(SourceCodeAvailability sourceCodeAvailability) {
		this.sourceCodeAvailability = sourceCodeAvailability;
	}

	public String getTechStacks() {
		return techStacks;
	}

	public void setTechStacks(String techStacks) {
		this.techStacks = techStacks;
	}

	public String getProjectUrl() {
		return projectUrl;
	}

	public void setProjectUrl(String projectUrl) {
		this.projectUrl = projectUrl;
	}

	public String getLiveUrl() {
		return liveUrl;
	}

	public void setLiveUrl(String liveUrl) {
		this.liveUrl = liveUrl;
	}

	public ProjectMediaType getMediaType() {
		return mediaType;
	}

	public void setMediaType(ProjectMediaType mediaType) {
		this.mediaType = mediaType;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}
}
