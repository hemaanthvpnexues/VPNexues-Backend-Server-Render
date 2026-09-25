package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpSendRequest(@NotBlank @Pattern(regexp = "^[+0-9 ]{6,20}$") String phone) {
}
