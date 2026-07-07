package com.sresthaa.publicui.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	List<Project> findAllByOrderByStartDateDesc();
}
