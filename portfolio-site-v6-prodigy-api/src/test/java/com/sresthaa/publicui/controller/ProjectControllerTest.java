package com.sresthaa.publicui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.security.JwtService;
import com.sresthaa.publicui.model.AffiliationType;
import com.sresthaa.publicui.model.CollabMode;
import com.sresthaa.publicui.model.Project;
import com.sresthaa.publicui.model.ProjectMediaType;
import com.sresthaa.publicui.model.ProjectStatus;
import com.sresthaa.publicui.model.SourceCodeAvailability;
import com.sresthaa.publicui.repository.ProjectRepository;
import com.sresthaa.storage.R2StorageService;

// R2StorageService is mocked - see ImageUploadControllerTest for why (real one talks to an
// actual Cloudflare bucket over the network).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private AdminAccountRepository adminAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private R2StorageService r2StorageService;

	private String token;

	@BeforeEach
	void createAdminToken() {
		adminAccountRepository.save(new AdminAccount("test-admin", passwordEncoder.encode("correct-horse")));
		token = jwtService.issueToken("test-admin");
	}

	private Project videoProject(String name, Instant startDate) {
		return new Project(name, "A short description", null, ProjectStatus.IN_PROGRESS, startDate, null,
				CollabMode.SOLO, "Independent", AffiliationType.INDEPENDENT, SourceCodeAvailability.OPEN_SOURCE,
				"Vue, Spring Boot", null, null, ProjectMediaType.VIDEO, null, "https://www.youtube.com/watch?v=abc");
	}

	@Test
	void listProjectsIsPublicAndReturnsNewestStartDateFirst() throws Exception {
		projectRepository.save(videoProject("First", Instant.parse("2025-01-01T00:00:00Z")));
		projectRepository.save(videoProject("Second", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(get("/api/projects"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Second"))
				.andExpect(jsonPath("$[1].name").value("First"));
	}

	@Test
	void createRejectsMissingToken() throws Exception {
		mockMvc.perform(post("/api/admin/projects")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void createRejectsBlankName() throws Exception {
		mockMvc.perform(post("/api/admin/projects")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(baseVideoJson("  ")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createImageProjectRejectsMissingImage() throws Exception {
		mockMvc.perform(post("/api/admin/projects")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"A project\",\"shortDesc\":\"desc\",\"statusFlag\":\"PLANNING\","
						+ "\"startDate\":\"2026-01-01T00:00:00Z\",\"collabMode\":\"SOLO\",\"affiliation\":\"Independent\","
						+ "\"affiliationType\":\"INDEPENDENT\",\"sourceCodeAvailability\":\"OPEN_SOURCE\","
						+ "\"techStacks\":\"Vue\",\"mediaType\":\"IMAGE\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createVideoProjectRejectsMissingVideoUrl() throws Exception {
		mockMvc.perform(post("/api/admin/projects")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"A project\",\"shortDesc\":\"desc\",\"statusFlag\":\"PLANNING\","
						+ "\"startDate\":\"2026-01-01T00:00:00Z\",\"collabMode\":\"SOLO\",\"affiliation\":\"Independent\","
						+ "\"affiliationType\":\"INDEPENDENT\",\"sourceCodeAvailability\":\"OPEN_SOURCE\","
						+ "\"techStacks\":\"Vue\",\"mediaType\":\"VIDEO\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsCompletedProjectMissingEndDate() throws Exception {
		mockMvc.perform(post("/api/admin/projects")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(baseVideoJson("A project").replace("\"statusFlag\":\"PLANNING\"", "\"statusFlag\":\"COMPLETED\"")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createVideoProjectSucceedsAndPersists() throws Exception {
		mockMvc.perform(post("/api/admin/projects")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(baseVideoJson("A project")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("A project"))
				.andExpect(jsonPath("$.videoUrl").value("https://www.youtube.com/watch?v=abc"));

		assertEquals(1, projectRepository.findAllByOrderByStartDateDesc().size());
	}

	@Test
	void createImageProjectSucceedsAndPersists() throws Exception {
		mockMvc.perform(post("/api/admin/projects")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"A project\",\"shortDesc\":\"desc\",\"statusFlag\":\"PLANNING\","
						+ "\"startDate\":\"2026-01-01T00:00:00Z\",\"collabMode\":\"SOLO\",\"affiliation\":\"Independent\","
						+ "\"affiliationType\":\"INDEPENDENT\",\"sourceCodeAvailability\":\"OPEN_SOURCE\","
						+ "\"techStacks\":\"Vue\",\"mediaType\":\"IMAGE\",\"image\":\"https://cdn.example/full.webp\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").value("https://cdn.example/full.webp"));

		assertEquals(1, projectRepository.findAllByOrderByStartDateDesc().size());
	}

	@Test
	void updateModifiesExistingProject() throws Exception {
		Project saved = projectRepository.save(videoProject("Original", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(put("/api/admin/projects/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(baseVideoJson("Updated")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated"));

		assertEquals("Updated", projectRepository.findById(saved.getId()).orElseThrow().getName());
	}

	@Test
	void updateRejectsUnknownId() throws Exception {
		mockMvc.perform(put("/api/admin/projects/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(baseVideoJson("Updated")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRemovesProject() throws Exception {
		Project saved = projectRepository.save(videoProject("Original", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(delete("/api/admin/projects/" + saved.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertTrue(projectRepository.findById(saved.getId()).isEmpty());
	}

	@Test
	void deleteRejectsUnknownId() throws Exception {
		mockMvc.perform(delete("/api/admin/projects/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteImageProjectRemovesR2Object() throws Exception {
		Project saved = projectRepository.save(new Project("A project", "desc", null, ProjectStatus.PLANNING,
				Instant.parse("2026-01-01T00:00:00Z"), null, CollabMode.SOLO, "Independent", AffiliationType.INDEPENDENT,
				SourceCodeAvailability.OPEN_SOURCE, "Vue", null, null, ProjectMediaType.IMAGE,
				"https://cdn.example/full.webp", null));

		mockMvc.perform(delete("/api/admin/projects/" + saved.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		verify(r2StorageService).delete("https://cdn.example/full.webp");
	}

	@Test
	void deleteVideoProjectNeverCallsR2() throws Exception {
		Project saved = projectRepository.save(videoProject("Original", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(delete("/api/admin/projects/" + saved.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		verify(r2StorageService, never()).delete(anyString());
	}

	@Test
	void updateReplacingImageDeletesOnlyTheOldR2Object() throws Exception {
		Project saved = projectRepository.save(new Project("A project", "desc", null, ProjectStatus.PLANNING,
				Instant.parse("2026-01-01T00:00:00Z"), null, CollabMode.SOLO, "Independent", AffiliationType.INDEPENDENT,
				SourceCodeAvailability.OPEN_SOURCE, "Vue", null, null, ProjectMediaType.IMAGE,
				"https://cdn.example/old-full.webp", null));

		mockMvc.perform(put("/api/admin/projects/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"A project\",\"shortDesc\":\"desc\",\"statusFlag\":\"PLANNING\","
						+ "\"startDate\":\"2026-01-01T00:00:00Z\",\"collabMode\":\"SOLO\",\"affiliation\":\"Independent\","
						+ "\"affiliationType\":\"INDEPENDENT\",\"sourceCodeAvailability\":\"OPEN_SOURCE\","
						+ "\"techStacks\":\"Vue\",\"mediaType\":\"IMAGE\",\"image\":\"https://cdn.example/new-full.webp\"}"))
				.andExpect(status().isOk());

		verify(r2StorageService).delete("https://cdn.example/old-full.webp");
		verify(r2StorageService, never()).delete("https://cdn.example/new-full.webp");
	}

	private String baseVideoJson(String name) {
		return "{\"name\":\"" + name + "\",\"shortDesc\":\"desc\",\"statusFlag\":\"PLANNING\","
				+ "\"startDate\":\"2026-01-01T00:00:00Z\",\"collabMode\":\"SOLO\",\"affiliation\":\"Independent\","
				+ "\"affiliationType\":\"INDEPENDENT\",\"sourceCodeAvailability\":\"OPEN_SOURCE\","
				+ "\"techStacks\":\"Vue\",\"mediaType\":\"VIDEO\",\"videoUrl\":\"https://www.youtube.com/watch?v=abc\"}";
	}
}
