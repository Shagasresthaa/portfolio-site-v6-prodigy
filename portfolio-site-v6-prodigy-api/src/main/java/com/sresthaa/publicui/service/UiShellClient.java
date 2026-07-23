package com.sresthaa.publicui.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// Fetches ui's live index.html rather than keeping a copy here, which would go stale as soon
// as ui redeploys (content-hashed JS/CSS filenames). Split out so tests can @MockitoBean it.
@Service
public class UiShellClient {

	private final RestClient restClient;

	public UiShellClient(@Value("${ui.internal-base-url}") String uiBaseUrl) {
		this.restClient = RestClient.builder().baseUrl(uiBaseUrl).build();
	}

	public String fetchIndexHtml() {
		return restClient.get().uri("/index.html").retrieve().body(String.class);
	}
}
