package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CreateTeamMemberRequest;
import com.vpnexues.svc.dto.ResponsibilityItem;
import com.vpnexues.svc.dto.TeamMemberDto;
import com.vpnexues.svc.dto.UpdateTeamMemberRequest;
import com.vpnexues.svc.entity.TeamMember;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.TeamMemberRepository;
import com.vpnexues.svc.util.Slugify;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final ObjectMapper objectMapper;

    /** Admin list: everything, including inactive members. */
    @Transactional(readOnly = true)
    public List<TeamMemberDto> listAll() {
        return teamMemberRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::toDto)
                .toList();
    }

    /** Public /team list: active members only. */
    @Transactional(readOnly = true)
    public List<TeamMemberDto> listActive() {
        return teamMemberRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public TeamMemberDto create(CreateTeamMemberRequest req) {
        TeamMember member = new TeamMember();
        member.setSlug(uniqueSlug(req.name()));
        apply(member, req.name(), req.role(), req.photoUrl(), req.description(), req.isDirector());
        member.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : nextOrder());
        member.setActive(req.active() == null || req.active());
        member.setResponsibilities(serializeResponsibilities(req.responsibilities()));
        member.setKeySkills(serializeSkills(req.keySkills()));
        return toDto(teamMemberRepository.save(member));
    }

    public TeamMemberDto update(UUID id, UpdateTeamMemberRequest req) {
        TeamMember member = getEntity(id);
        // slug stays as-is: the public /team/:slug URL must not break on a rename.
        apply(member, req.name(), req.role(), req.photoUrl(), req.description(), req.isDirector());
        if (req.displayOrder() != null) {
            member.setDisplayOrder(req.displayOrder());
        }
        // null keeps the stored flag so an old client can't silently deactivate members.
        if (req.active() != null) {
            member.setActive(req.active());
        }
        // null keeps stored detail lists; a supplied (possibly empty) list replaces them.
        if (req.responsibilities() != null) {
            member.setResponsibilities(serializeResponsibilities(req.responsibilities()));
        }
        if (req.keySkills() != null) {
            member.setKeySkills(serializeSkills(req.keySkills()));
        }
        return toDto(teamMemberRepository.save(member));
    }

    public void delete(UUID id) {
        if (!teamMemberRepository.existsById(id)) {
            throw new NotFoundException("Team member not found: " + id);
        }
        teamMemberRepository.deleteById(id);
    }

    private void apply(TeamMember member, String name, String role, String photoUrl, String description, Boolean isDirector) {
        member.setName(name.trim());
        member.setPhotoUrl(photoUrl);
        member.setDescription(description);
        member.setDirector(Boolean.TRUE.equals(isDirector));
        String trimmedRole = role == null || role.isBlank() ? null : role.trim();
        if (trimmedRole == null && !member.isDirector()) {
            throw new BadRequestException("Role is required unless the member is a director.");
        }
        member.setRole(trimmedRole);
    }

    /** Slugify on create; on the (rare) duplicate, append -2, -3, ... until unique. */
    private String uniqueSlug(String name) {
        String base = Slugify.slugify(name);
        if (base.isEmpty()) {
            base = "member";
        }
        String slug = base;
        int suffix = 2;
        while (teamMemberRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private int nextOrder() {
        List<TeamMember> all = teamMemberRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc();
        return all.isEmpty() ? 0 : all.get(all.size() - 1).getDisplayOrder() + 1;
    }

    private TeamMember getEntity(UUID id) {
        return teamMemberRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Team member not found: " + id));
    }

    /** null in → null out (unset); items with both fields blank are dropped. */
    private String serializeResponsibilities(List<ResponsibilityItem> items) {
        if (items == null) {
            return null;
        }
        List<ResponsibilityItem> clean = items.stream()
                .filter(item -> item != null)
                .map(item -> new ResponsibilityItem(trimOrNull(item.title()), trimOrNull(item.description())))
                .filter(item -> item.title() != null || item.description() != null)
                .toList();
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (tools.jackson.core.JacksonException ex) {
            throw new BadRequestException("Responsibilities could not be saved.");
        }
    }

    private String serializeSkills(List<String> skills) {
        if (skills == null) {
            return null;
        }
        List<String> clean = skills.stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .map(String::trim)
                .toList();
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (tools.jackson.core.JacksonException ex) {
            throw new BadRequestException("Key skills could not be saved.");
        }
    }

    /** Stored JSON → DTO list; null/blank stays null so the public page uses its defaults. */
    private List<ResponsibilityItem> deserializeResponsibilities(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return List.of(objectMapper.readValue(json, ResponsibilityItem[].class));
        } catch (tools.jackson.core.JacksonException ex) {
            return List.of();
        }
    }

    private List<String> deserializeSkills(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (tools.jackson.core.JacksonException ex) {
            return List.of();
        }
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TeamMemberDto toDto(TeamMember m) {
        return new TeamMemberDto(
                m.getId(),
                m.getSlug(),
                m.getName(),
                m.getRole(),
                m.getPhotoUrl(),
                m.getDescription(),
                m.isDirector(),
                m.getDisplayOrder(),
                m.isActive(),
                deserializeResponsibilities(m.getResponsibilities()),
                deserializeSkills(m.getKeySkills()),
                m.getCreatedAt());
    }
}
