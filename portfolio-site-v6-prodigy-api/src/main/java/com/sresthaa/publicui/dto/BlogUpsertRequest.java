package com.sresthaa.publicui.dto;

public record BlogUpsertRequest(String slug, String title, String excerpt, String content, String coverImage,
		boolean published, String tags) {
}
