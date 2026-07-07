package com.sresthaa.publicui.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.BlogImage;

public interface BlogImageRepository extends JpaRepository<BlogImage, UUID> {

	List<BlogImage> findAllByOrderByCreatedAtDesc();
}
