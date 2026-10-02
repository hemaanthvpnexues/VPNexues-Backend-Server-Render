package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.CreateTeamMemberRequest;
import com.vpnexues.svc.dto.ResponsibilityItem;
import com.vpnexues.svc.dto.TeamMemberDto;
import com.vpnexues.svc.dto.UpdateTeamMemberRequest;
import com.vpnexues.svc.entity.TeamMember;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.repository.TeamMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Unit tests for the team-member rules: slug generation, role/director validation, stable URLs. */
class TeamMemberServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TeamMemberRepository repository;
    private TeamMemberService service;

    @BeforeEach
    void setUp() {
        repository = mock(TeamMemberRepository.class);
        when(repository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findAllByOrderByDisplayOrderAscCreatedAtAsc()).thenReturn(List.of());
        service = new TeamMemberService(repository, MAPPER);
    }

    @Test
    void createSlugifiesNameAndAppendsSuffixOnCollision() {
        when(repository.existsBySlug("sathish-r")).thenReturn(true);
        when(repository.existsBySlug("sathish-r-2")).thenReturn(false);

        TeamMemberDto dto = service.create(
                new CreateTeamMemberRequest("  Sathish R ", "Web Developer", null, null, false, null, null, null, null));

        assertEquals("sathish-r-2", dto.slug());
        assertEquals("Sathish R", dto.name());
        assertEquals(0, dto.displayOrder());
        assertTrue(dto.active(), "a null active flag defaults to visible");
    }

    @Test
    void roleIsRequiredUnlessMemberIsDirector() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(
                new CreateTeamMemberRequest("New Person", "  ", null, null, false, null, null, null, null)));
        assertTrue(ex.getMessage().contains("Role"));

        TeamMemberDto director = service.create(
                new CreateTeamMemberRequest("The Boss", null, null, null, true, null, null, null, null));
        assertTrue(director.isDirector());
        assertNull(director.role());
    }

    @Test
    void updateKeepsSlugAndDisplayOrderWhenNotSupplied() {
        TeamMember existing = new TeamMember();
        existing.setId(UUID.randomUUID());
        existing.setSlug("libin-k");
        existing.setName("Libin K");
        existing.setDisplayOrder(7);
        UUID id = existing.getId();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        TeamMemberDto dto = service.update(id, new UpdateTeamMemberRequest(
                "Libin Kumar", "Module Lead", "/images/team/libin.jpg", "New bio", false, null, null, null, null));

        assertEquals("libin-k", dto.slug(), "public /team/:slug URL must not break on a rename");
        assertEquals("Libin Kumar", dto.name());
        assertEquals(7, dto.displayOrder());
        assertTrue(dto.active(), "null active on update keeps the stored flag");
    }

    @Test
    void updateDeactivatesWhenActiveFlagIsFalse() {
        TeamMember existing = new TeamMember();
        existing.setId(UUID.randomUUID());
        existing.setSlug("libin-k");
        existing.setName("Libin K");
        existing.setDisplayOrder(7);
        UUID id = existing.getId();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        TeamMemberDto dto = service.update(id, new UpdateTeamMemberRequest(
                "Libin K", "Module Lead", null, null, false, null, false, null, null));

        assertEquals(false, dto.active());
    }

    @Test
    void createStoresResponsibilitiesAndSkills() {
        List<ResponsibilityItem> responsibilities = List.of(
                new ResponsibilityItem(" Task Planning ", "Break requirements into tasks."),
                new ResponsibilityItem("", "   "));
        TeamMemberDto dto = service.create(new CreateTeamMemberRequest(
                "Libin K", "Team Lead", null, null, false, null, null, responsibilities, List.of("Leadership", "  ", "Git Workflow")));

        assertEquals(1, dto.responsibilities().size(), "blank rows are dropped");
        assertEquals("Task Planning", dto.responsibilities().get(0).title());
        assertEquals("Break requirements into tasks.", dto.responsibilities().get(0).description());
        assertEquals(List.of("Leadership", "Git Workflow"), dto.keySkills());
    }

    @Test
    void updateReplacesOrKeepsDetailLists() {
        TeamMember existing = new TeamMember();
        existing.setId(UUID.randomUUID());
        existing.setSlug("libin-k");
        existing.setName("Libin K");
        existing.setRole("Team Lead");
        existing.setResponsibilities("[{\"title\":\"Old\",\"description\":\"Row\"}]");
        existing.setKeySkills("[\"Old Skill\"]");
        UUID id = existing.getId();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        TeamMemberDto kept = service.update(id, new UpdateTeamMemberRequest(
                "Libin K", "Team Lead", null, null, false, null, null, null, null));
        assertEquals(1, kept.responsibilities().size(), "null list keeps the stored rows");
        assertEquals("Old", kept.responsibilities().get(0).title());
        assertEquals(List.of("Old Skill"), kept.keySkills());

        TeamMemberDto replaced = service.update(id, new UpdateTeamMemberRequest(
                "Libin K", "Team Lead", null, null, false, null, null,
                List.of(new ResponsibilityItem("Mentoring", "Guide the team.")), List.of()));
        assertEquals("Mentoring", replaced.responsibilities().get(0).title());
        assertEquals(List.of(), replaced.keySkills(), "an empty list clears stored skills");
    }
}
