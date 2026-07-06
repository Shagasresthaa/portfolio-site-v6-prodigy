package com.sresthaa.publicui.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.HomeContent;

public interface HomeContentRepository extends JpaRepository<HomeContent, UUID> {
}
