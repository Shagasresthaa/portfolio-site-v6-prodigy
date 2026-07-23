package com.sresthaa.publicui.dto;

// url is already an uploaded R2 object - this only registers it in the library, no upload here.
public record BlogImageCreateRequest(String url, String altText) {
}
