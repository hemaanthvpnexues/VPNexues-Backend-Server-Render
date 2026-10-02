package com.vpnexues.svc.dto;

/** Response of POST /api/admin/uploads/photo — server-relative path of the stored file. */
public record UploadPhotoResponseDto(String url) {
}
