package com.sresthaa.admin.dto;

public record ChangeUsernameRequest(String currentPassword, String newUsername) {
}
