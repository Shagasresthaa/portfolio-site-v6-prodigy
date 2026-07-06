package com.sresthaa.admin.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.admin.model.TotpCredential;

public interface TotpCredentialRepository extends JpaRepository<TotpCredential, UUID> {

	Optional<TotpCredential> findByAdminAccountId(UUID adminAccountId);
}
