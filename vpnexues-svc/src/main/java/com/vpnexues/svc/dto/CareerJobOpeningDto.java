package com.vpnexues.svc.dto;

import java.util.List;

/**
 * One named role inside a multi-opening job (e.g. the "Sales & Marketing Executive"
 * card carries three openings). Single-role jobs store no openings at all and are
 * rendered from the job's own responsibilities instead.
 */
public record CareerJobOpeningDto(String role, String type, List<String> responsibilities) {
}
