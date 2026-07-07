package com.sresthaa.publicui.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.Blog;

public interface BlogRepository extends JpaRepository<Blog, UUID> {

	List<Blog> findAllByPublishedTrueOrderByPublishedAtDesc();

	Optional<Blog> findBySlug(String slug);

	Optional<Blog> findBySlugAndPublishedTrue(String slug);
}
