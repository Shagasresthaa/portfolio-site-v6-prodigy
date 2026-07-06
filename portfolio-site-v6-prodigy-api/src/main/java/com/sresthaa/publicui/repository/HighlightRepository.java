package com.sresthaa.publicui.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.Highlight;

public interface HighlightRepository extends JpaRepository<Highlight, UUID> {

	List<Highlight> findAllByOrderByCreatedAtDesc();
}
