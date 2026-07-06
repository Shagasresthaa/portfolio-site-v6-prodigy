package com.sresthaa.admin.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.admin.model.AdminAccount;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {

	Optional<AdminAccount> findByUsername(String username);
}
