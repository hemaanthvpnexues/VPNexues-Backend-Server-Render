package com.vpnexues.svc.dto;

import java.util.UUID;

public record AddressDto(
        UUID id,
        String label,
        String name,
        String phone,
        String addressLine,
        String flat,
        String landmark,
        String city,
        String pincode,
        String state,
        boolean isDefault,
        Double lat,
        Double lng) {
}
