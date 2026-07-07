package com.sresthaa.publicui.dto;

// url is already an uploaded R2 object (via the generic /api/admin/uploads/image endpoint,
// category "blog") - this just registers it in the reusable library, it doesn't upload anything
// itself.
public record BlogImageCreateRequest(String url, String altText) {
}
