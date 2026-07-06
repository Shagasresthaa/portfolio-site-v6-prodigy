package com.sresthaa.publicui.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.ContactForm;

public interface ContactFormRepository extends JpaRepository<ContactForm, UUID> {

	List<ContactForm> findAllByOrderByCreatedAtDesc();
}
