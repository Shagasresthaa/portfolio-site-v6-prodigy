package com.sresthaa.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.admin.model.TotpBackupCode;

public interface TotpBackupCodeRepository extends JpaRepository<TotpBackupCode, UUID> {

	List<TotpBackupCode> findByTotpCredentialId(UUID totpCredentialId);
}
