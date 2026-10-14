package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderFeedbackRequest(
        @Size(max = 30) String orderNumber,
        @Min(1) @Max(5) int rating,
        @Size(max = 8) List<@Size(max = 60) String> tags,
        @Size(max = 2000) String comment,
        @Size(max = 120) String headline,
        @Size(max = 4) String countryCode,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 30) String contactPhone) {}
