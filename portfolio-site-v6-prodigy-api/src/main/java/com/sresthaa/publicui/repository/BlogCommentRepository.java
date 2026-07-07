package com.sresthaa.publicui.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sresthaa.publicui.model.BlogComment;

public interface BlogCommentRepository extends JpaRepository<BlogComment, UUID> {

	List<BlogComment> findAllByBlogIdOrderByCreatedAtDesc(UUID blogId);

	void deleteAllByBlogId(UUID blogId);
}
