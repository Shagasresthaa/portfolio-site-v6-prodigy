package com.sresthaa.admin.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
