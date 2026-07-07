package com.sresthaa.publicui.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.publicui.dto.ProjectSummary;
import com.sresthaa.publicui.dto.ProjectUpsertRequest;
import com.sresthaa.publicui.model.Project;
import com.sresthaa.publicui.model.ProjectMediaType;
import com.sresthaa.publicui.model.ProjectStatus;
import com.sresthaa.publicui.repository.ProjectRepository;
import com.sresthaa.storage.R2StorageService;

@Service
public class ProjectService {

	private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

	private final ProjectRepository projectRepository;
	private final R2StorageService r2StorageService;

	public ProjectService(ProjectRepository projectRepository, R2StorageService r2StorageService) {
		this.projectRepository = projectRepository;
		this.r2StorageService = r2StorageService;
	}

	public List<ProjectSummary> listAll() {
		return projectRepository.findAllByOrderByStartDateDesc().stream().map(ProjectSummary::from).toList();
	}

	public ProjectSummary create(ProjectUpsertRequest request) {
		Project project = new Project(null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null);
		applyAndValidate(project, request);
		return ProjectSummary.from(projectRepository.save(project));
	}

	public ProjectSummary update(UUID id, ProjectUpsertRequest request) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such project"));

		String previousImage = project.getImage();

		applyAndValidate(project, request);
		ProjectSummary summary = ProjectSummary.from(projectRepository.save(project));

		// Replaced (or dropped, e.g. switched to VIDEO) - the old object is now orphaned in R2.
		if (previousImage != null && !previousImage.equals(project.getImage())) {
			deleteFromStorage(previousImage);
		}

		return summary;
	}

	public void delete(UUID id) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such project"));

		projectRepository.deleteById(id);

		deleteFromStorage(project.getImage());
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

	private void applyAndValidate(Project project, ProjectUpsertRequest request) {
		String name = request.name() == null ? "" : request.name().trim();
		String shortDesc = request.shortDesc() == null ? "" : request.shortDesc().trim();
		String affiliation = request.affiliation() == null ? "" : request.affiliation().trim();
		String techStacks = request.techStacks() == null ? "" : request.techStacks().trim();

		if (name.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
		}
		if (shortDesc.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Short description is required");
		}
		if (affiliation.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Affiliation is required");
		}
		if (techStacks.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one tech stack is required");
		}
		if (request.statusFlag() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
		}
		if (request.startDate() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is required");
		}
		if (request.collabMode() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collaboration mode is required");
		}
		if (request.affiliationType() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Affiliation type is required");
		}
		if (request.sourceCodeAvailability() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source code availability is required");
		}
		if (request.mediaType() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media type is required");
		}
		if ((request.statusFlag() == ProjectStatus.COMPLETED || request.statusFlag() == ProjectStatus.ARCHIVED)
				&& request.endDate() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"End date is required for completed/archived projects");
		}

		if (request.mediaType() == ProjectMediaType.IMAGE) {
			if (blank(request.image())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required");
			}
		} else if (blank(request.videoUrl())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video URL is required");
		}

		project.setName(name);
		project.setShortDesc(shortDesc);
		project.setLongDesc(blankToNull(request.longDesc()));
		project.setStatusFlag(request.statusFlag());
		project.setStartDate(request.startDate());
		project.setEndDate(request.endDate());
		project.setCollabMode(request.collabMode());
		project.setAffiliation(affiliation);
		project.setAffiliationType(request.affiliationType());
		project.setSourceCodeAvailability(request.sourceCodeAvailability());
		project.setTechStacks(techStacks);
		project.setProjectUrl(blankToNull(request.projectUrl()));
		project.setLiveUrl(blankToNull(request.liveUrl()));
		project.setMediaType(request.mediaType());

		if (request.mediaType() == ProjectMediaType.IMAGE) {
			project.setImage(request.image().trim());
			project.setVideoUrl(null);
		} else {
			project.setVideoUrl(request.videoUrl().trim());
			project.setImage(null);
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
