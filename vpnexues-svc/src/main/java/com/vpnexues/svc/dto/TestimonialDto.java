package com.vpnexues.svc.dto;

import java.util.UUID;

public record TestimonialDto(
        UUID id, String name, String role, String location, int rating, String quote, String photoUrl, boolean active) {
}
