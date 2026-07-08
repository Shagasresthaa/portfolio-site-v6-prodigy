package com.sresthaa.publicui.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

	List<Certificate> findAllByOrderByCreatedAtDesc();
}
