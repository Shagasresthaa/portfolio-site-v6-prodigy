package com.sresthaa.publicui.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.publicui.dto.ProjectSummary;
import com.sresthaa.publicui.dto.ProjectUpsertRequest;
import com.sresthaa.publicui.service.ProjectService;

@RestController
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	// Every project is always public - no draft/published distinction - so this same endpoint
	// serves both the public site and the admin manager.
	@GetMapping("/api/projects")
	public List<ProjectSummary> listProjects() {
		return projectService.listAll();
	}

	@PostMapping("/api/admin/projects")
	public ProjectSummary create(@RequestBody ProjectUpsertRequest request) {
		return projectService.create(request);
	}

	@PutMapping("/api/admin/projects/{id}")
	public ProjectSummary update(@PathVariable UUID id, @RequestBody ProjectUpsertRequest request) {
		return projectService.update(id, request);
	}

	@DeleteMapping("/api/admin/projects/{id}")
	public void delete(@PathVariable UUID id) {
		projectService.delete(id);
	}
}
