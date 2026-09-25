package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAddressRequest(
        @NotBlank String label,
        @NotBlank String name,
        @NotBlank String phone,
        @NotBlank String addressLine,
        String flat,
        String landmark,
        String city,
        String pincode,
        String state,
        boolean isDefault,
        Double lat,
        Double lng) {
}
