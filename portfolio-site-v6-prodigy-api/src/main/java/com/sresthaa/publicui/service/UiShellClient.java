package com.sresthaa.publicui.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// Fetches the ui service's own currently-deployed index.html rather than keeping a duplicate
// copy here - the built JS/CSS filenames are content-hashed and change on every ui deploy, and
// the ui/api images are built and rolled out independently, so any copy kept in the api would go
// stale. Split out from BlogPreviewService so tests can @MockitoBean this HTTP-fetching bit
// without needing to mock RestClient's fluent chain directly (same reasoning as R2StorageService
// being mocked in upload-related tests).
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
