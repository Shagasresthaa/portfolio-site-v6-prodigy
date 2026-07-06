package com.sresthaa.admin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.admin.model.WebAuthnCredential;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, UUID> {

	List<WebAuthnCredential> findByAdminAccountId(UUID adminAccountId);

	Optional<WebAuthnCredential> findByCredentialId(byte[] credentialId);
}
