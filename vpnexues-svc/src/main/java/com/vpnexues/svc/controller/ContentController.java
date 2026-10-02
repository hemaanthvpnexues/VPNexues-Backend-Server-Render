package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.NewsArticleDto;
import com.vpnexues.svc.dto.TeamMemberDto;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.service.NewsService;
import com.vpnexues.svc.service.TeamMemberService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated content endpoints backing the public /team and /news pages. */
@RestController
@RequiredArgsConstructor
public class ContentController {

    private final TeamMemberService teamMemberService;
    private final NewsService newsService;

    @GetMapping("/api/team-members")
    public List<TeamMemberDto> teamMembers() {
        return teamMemberService.listActive();
    }

    @GetMapping("/api/news")
    public List<NewsArticleDto> news() {
        return newsService.listPublished();
    }

    @GetMapping("/api/news/{id}")
    public NewsArticleDto newsById(@PathVariable String id) {
        // Not a UUID (e.g. an old static link) — 404 as "no such article" instead of a 500.
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("News article not found: " + id);
        }
        return newsService.getPublishedById(uuid);
    }
}
