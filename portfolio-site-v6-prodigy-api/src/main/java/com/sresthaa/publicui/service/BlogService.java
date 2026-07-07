package com.sresthaa.publicui.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.publicui.dto.BlogCommentRequest;
import com.sresthaa.publicui.dto.BlogCommentSummary;
import com.sresthaa.publicui.dto.BlogReactionCounts;
import com.sresthaa.publicui.dto.BlogReactionRequest;
import com.sresthaa.publicui.dto.BlogSummary;
import com.sresthaa.publicui.dto.BlogUpsertRequest;
import com.sresthaa.publicui.model.Blog;
import com.sresthaa.publicui.model.BlogComment;
import com.sresthaa.publicui.model.ReactionVote;
import com.sresthaa.publicui.repository.BlogCommentRepository;
import com.sresthaa.publicui.repository.BlogRepository;
import com.sresthaa.storage.R2StorageService;

@Service
public class BlogService {

	private static final Logger log = LoggerFactory.getLogger(BlogService.class);

	private static final int COMMENT_NAME_MAX_LENGTH = 100;
	private static final int COMMENT_CONTENT_MAX_LENGTH = 2000;

	private final BlogRepository blogRepository;
	private final BlogCommentRepository blogCommentRepository;
	private final R2StorageService r2StorageService;

	public BlogService(BlogRepository blogRepository, BlogCommentRepository blogCommentRepository,
			R2StorageService r2StorageService) {
		this.blogRepository = blogRepository;
		this.blogCommentRepository = blogCommentRepository;
		this.r2StorageService = r2StorageService;
	}

	public List<BlogSummary> listPublished() {
		return blogRepository.findAllByPublishedTrueOrderByPublishedAtDesc().stream().map(BlogSummary::from).toList();
	}

	public List<BlogSummary> listAll() {
		return blogRepository.findAll().stream().map(BlogSummary::from).toList();
	}

	public BlogSummary getPublishedBySlug(String slug) {
		return BlogSummary.from(findPublishedBlogBySlug(slug));
	}

	public BlogSummary create(BlogUpsertRequest request) {
		Blog blog = new Blog(null, null, null, null, null, false, null, null, 0, 0);
		applyAndValidate(null, blog, request);
		return BlogSummary.from(blogRepository.save(blog));
	}

	public BlogSummary update(UUID id, BlogUpsertRequest request) {
		Blog blog = blogRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such post"));

		String previousCoverImage = blog.getCoverImage();

		applyAndValidate(id, blog, request);
		BlogSummary summary = BlogSummary.from(blogRepository.save(blog));

		if (previousCoverImage != null && !previousCoverImage.equals(blog.getCoverImage())) {
			deleteFromStorage(previousCoverImage);
		}

		return summary;
	}

	public void delete(UUID id) {
		Blog blog = blogRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such post"));

		blogCommentRepository.deleteAllByBlogId(id);
		blogRepository.deleteById(id);

		deleteFromStorage(blog.getCoverImage());
	}

	public List<BlogCommentSummary> listComments(String slug) {
		Blog blog = findPublishedBlogBySlug(slug);
		return blogCommentRepository.findAllByBlogIdOrderByCreatedAtDesc(blog.getId()).stream()
				.map(BlogCommentSummary::from).toList();
	}

	public BlogCommentSummary addComment(String slug, BlogCommentRequest request) {
		Blog blog = findPublishedBlogBySlug(slug);

		String name = blankToNull(request.name());
		if (name != null && name.length() > COMMENT_NAME_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Name must be " + COMMENT_NAME_MAX_LENGTH + " characters or fewer");
		}

		String content = request.content() == null ? "" : request.content().trim();
		if (content.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content is required");
		}
		if (content.length() > COMMENT_CONTENT_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Comment must be " + COMMENT_CONTENT_MAX_LENGTH + " characters or fewer");
		}

		BlogComment comment = new BlogComment(blog, name, content);
		return BlogCommentSummary.from(blogCommentRepository.save(comment));
	}

	// No per-visitor identity to dedup against by design (see memory:
	// no-user-accounts-by-design) - counts are adjusted blindly per the client-supplied
	// transition. Clamped at 0 so a stale/replayed "remove" can't drive a count negative.
	public BlogReactionCounts applyReaction(String slug, BlogReactionRequest request) {
		Blog blog = findPublishedBlogBySlug(slug);

		if (request.previousVote() == ReactionVote.LIKE) {
			blog.setLikeCount(Math.max(0, blog.getLikeCount() - 1));
		} else if (request.previousVote() == ReactionVote.DISLIKE) {
			blog.setDislikeCount(Math.max(0, blog.getDislikeCount() - 1));
		}

		if (request.vote() == ReactionVote.LIKE) {
			blog.setLikeCount(blog.getLikeCount() + 1);
		} else if (request.vote() == ReactionVote.DISLIKE) {
			blog.setDislikeCount(blog.getDislikeCount() + 1);
		}

		Blog saved = blogRepository.save(blog);
		return new BlogReactionCounts(saved.getLikeCount(), saved.getDislikeCount());
	}

	private Blog findPublishedBlogBySlug(String slug) {
		return blogRepository.findBySlugAndPublishedTrue(slug)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such post"));
	}

	// Best-effort: an R2 hiccup here shouldn't fail (or roll back) a content change the admin
	// already confirmed - worst case is a harmless orphaned object, logged for manual cleanup.
	private void deleteFromStorage(String url) {
		if (url == null) {
			return;
		}
		try {
			r2StorageService.delete(url);
		} catch (RuntimeException e) {
			log.warn("Failed to delete R2 object at {}", url, e);
		}
	}

	private void applyAndValidate(UUID currentId, Blog blog, BlogUpsertRequest request) {
		String slug = request.slug() == null ? "" : request.slug().trim();
		String title = request.title() == null ? "" : request.title().trim();
		String excerpt = request.excerpt() == null ? "" : request.excerpt().trim();
		String content = request.content() == null ? "" : request.content().trim();

		if (slug.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug is required");
		}
		if (title.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
		}
		if (excerpt.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excerpt is required");
		}
		if (content.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
		}

		blogRepository.findBySlug(slug).ifPresent(existing -> {
			if (currentId == null || !existing.getId().equals(currentId)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug already in use");
			}
		});

		blog.setSlug(slug);
		blog.setTitle(title);
		blog.setExcerpt(excerpt);
		blog.setContent(content);
		blog.setCoverImage(blankToNull(request.coverImage()));
		blog.setTags(request.tags() == null ? "" : request.tags().trim());
		blog.setPublished(request.published());

		// First publish sets the timestamp permanently - unpublishing and republishing later
		// doesn't reset it, so a post's publish date reflects when it first went live.
		if (request.published() && blog.getPublishedAt() == null) {
			blog.setPublishedAt(Instant.now());
		}
	}

	private String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
