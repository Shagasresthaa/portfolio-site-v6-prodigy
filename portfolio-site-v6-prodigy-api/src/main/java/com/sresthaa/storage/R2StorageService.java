package com.sresthaa.storage;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// Cloudflare R2 is S3-compatible - this just points the AWS SDK's S3 client at R2's endpoint.
// forcePathStyle is required per R2's docs (bucket-in-path, not bucket-as-subdomain); region is
// the literal string "auto", also per R2's docs, not a real AWS region.
@Service
public class R2StorageService {

	private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
			"image/webp", ".webp",
			"image/png", ".png",
			"image/jpeg", ".jpg",
			"application/pdf", ".pdf");

	private final S3Client s3Client;
	private final String bucketName;
	private final String publicBaseUrl;

	public R2StorageService(@Value("${r2.access-key-id}") String accessKeyId,
			@Value("${r2.secret-access-key}") String secretAccessKey, @Value("${r2.bucket-name}") String bucketName,
			@Value("${r2.endpoint}") String endpoint, @Value("${r2.public-base-url}") String publicBaseUrl) {
		this.bucketName = bucketName;
		this.publicBaseUrl = publicBaseUrl;
		this.s3Client = S3Client.builder()
				.endpointOverride(URI.create(endpoint))
				.region(Region.of("auto"))
				.forcePathStyle(true)
				.credentialsProvider(
						StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
				.build();
	}

	// keyPrefix groups uploads by feature (e.g. "highlights", "projects") so the bucket doesn't
	// become one flat pile of files.
	public String upload(byte[] content, String contentType, String keyPrefix) {
		String extension = EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, "");
		String key = keyPrefix + "/" + UUID.randomUUID() + extension;

		s3Client.putObject(
				PutObjectRequest.builder().bucket(bucketName).key(key).contentType(contentType).build(),
				RequestBody.fromBytes(content));

		return publicBaseUrl + "/" + key;
	}

	// Takes the full public URL (as returned by upload()) rather than a bare key, since that's
	// what callers actually have on hand (it's what gets persisted on the entity).
	public void delete(String publicUrl) {
		if (publicUrl == null || !publicUrl.startsWith(publicBaseUrl + "/")) {
			return;
		}
		String key = publicUrl.substring(publicBaseUrl.length() + 1);
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
	}
}
