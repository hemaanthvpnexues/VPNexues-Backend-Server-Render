package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CareerJobDto;
import com.vpnexues.svc.dto.CreateCareerJobRequest;
import com.vpnexues.svc.dto.CreateGalleryImageRequest;
import com.vpnexues.svc.dto.CreateNewsArticleRequest;
import com.vpnexues.svc.dto.CreateTeamMemberRequest;
import com.vpnexues.svc.dto.GalleryImageDto;
import com.vpnexues.svc.dto.NewsArticleDto;
import com.vpnexues.svc.dto.TeamMemberDto;
import com.vpnexues.svc.dto.UpdateCareerJobRequest;
import com.vpnexues.svc.dto.UpdateGalleryImageRequest;
import com.vpnexues.svc.dto.UpdateNewsArticleRequest;
import com.vpnexues.svc.dto.UpdateTeamMemberRequest;
import com.vpnexues.svc.dto.UploadPhotoResponseDto;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.service.AdminAccessService;
import com.vpnexues.svc.service.CareerJobService;
import com.vpnexues.svc.service.ContentUploadService;
import com.vpnexues.svc.service.GalleryService;
import com.vpnexues.svc.service.NewsService;
import com.vpnexues.svc.service.TeamMemberService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin CRUD for the public /team, /news and /gallery content (admin JWT chain: /api/admin/**).
 * Restricted to the India sub-admin (and super admins): other country dashboards
 * do not expose this management UI, and the API enforces the same rule.
 */
@RestController
@RequiredArgsConstructor
public class AdminContentController {

    private final TeamMemberService teamMemberService;
    private final NewsService newsService;
    private final GalleryService galleryService;
    private final CareerJobService careerJobService;
    private final AdminUserRepository adminUserRepository;
    private final ContentUploadService contentUploadService;
    private final AdminAccessService accessService;

    /** Stores a photo on disk under /uploads/** and returns its URL (never a base64 blob).
     * The name (member name / article title) becomes the filename: /uploads/team/nageswaran-duraiswamy.jpg. */
    @PostMapping("/api/admin/uploads/photo")
    public UploadPhotoResponseDto uploadPhoto(@AuthenticationPrincipal UUID adminId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "replace", required = false) String replace,
            @RequestParam(value = "folder", required = false) String folder) throws IOException {
        requireContentEdit(adminId);
        return new UploadPhotoResponseDto(contentUploadService.storePhoto(file, name, replace, folder));
    }

    @GetMapping("/api/admin/team-members")
    public List<TeamMemberDto> listTeamMembers(@AuthenticationPrincipal UUID adminId) {
        requireContentRead(adminId);
        return teamMemberService.listAll();
    }

    @PostMapping("/api/admin/team-members")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamMemberDto createTeamMember(@AuthenticationPrincipal UUID adminId,
            @RequestBody @Valid CreateTeamMemberRequest req) {
        requireContentEdit(adminId);
        return teamMemberService.create(req);
    }

    @PutMapping("/api/admin/team-members/{id}")
    public TeamMemberDto updateTeamMember(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
            @RequestBody @Valid UpdateTeamMemberRequest req) {
        requireContentEdit(adminId);
        return teamMemberService.update(id, req);
    }

    @DeleteMapping("/api/admin/team-members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeamMember(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id) {
        requireContentDelete(adminId);
        teamMemberService.delete(id);
    }

    @GetMapping("/api/admin/news")
    public List<NewsArticleDto> listNews(@AuthenticationPrincipal UUID adminId) {
        requireContentRead(adminId);
        return newsService.listAll();
    }

    @PostMapping("/api/admin/news")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsArticleDto createNews(@AuthenticationPrincipal UUID adminId,
            @RequestBody @Valid CreateNewsArticleRequest req) {
        requireContentEdit(adminId);
        return newsService.create(req);
    }

    @PutMapping("/api/admin/news/{id}")
    public NewsArticleDto updateNews(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
            @RequestBody @Valid UpdateNewsArticleRequest req) {
        requireContentEdit(adminId);
        return newsService.update(id, req);
    }

    @DeleteMapping("/api/admin/news/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNews(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id) {
        requireContentDelete(adminId);
        newsService.delete(id);
    }

    @GetMapping("/api/admin/gallery-images")
    public List<GalleryImageDto> listGalleryImages(@AuthenticationPrincipal UUID adminId) {
        requireContentRead(adminId);
        return galleryService.listAll();
    }

    @PostMapping("/api/admin/gallery-images")
    @ResponseStatus(HttpStatus.CREATED)
    public GalleryImageDto createGalleryImage(@AuthenticationPrincipal UUID adminId,
            @RequestBody @Valid CreateGalleryImageRequest req) {
        requireContentEdit(adminId);
        return galleryService.create(req);
    }

    @PutMapping("/api/admin/gallery-images/{id}")
    public GalleryImageDto updateGalleryImage(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
            @RequestBody @Valid UpdateGalleryImageRequest req) {
        requireContentEdit(adminId);
        return galleryService.update(id, req);
    }

    @DeleteMapping("/api/admin/gallery-images/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGalleryImage(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id) {
        requireContentDelete(adminId);
        galleryService.delete(id);
    }

    @GetMapping("/api/admin/career-jobs")
    public List<CareerJobDto> listCareerJobs(@AuthenticationPrincipal UUID adminId) {
        requireCareersRead(adminId);
        return careerJobService.listAll();
    }

    @PostMapping("/api/admin/career-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public CareerJobDto createCareerJob(@AuthenticationPrincipal UUID adminId,
            @RequestBody @Valid CreateCareerJobRequest req) {
        requireCareersEdit(adminId);
        return careerJobService.create(req);
    }

    @PutMapping("/api/admin/career-jobs/{id}")
    public CareerJobDto updateCareerJob(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
            @RequestBody @Valid UpdateCareerJobRequest req) {
        requireCareersEdit(adminId);
        return careerJobService.update(id, req);
    }

    @DeleteMapping("/api/admin/career-jobs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCareerJob(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id) {
        requireCareersDelete(adminId);
        careerJobService.delete(id);
    }

    // ── Module/permission checks (super admin bypasses inside AdminAccessService) ──

    private void requireContentRead(UUID adminId) {
        requireContentManager(adminId);
        accessService.requireRead(adminId, AdminAccessService.M_CONTENT);
    }

    private void requireContentEdit(UUID adminId) {
        requireContentManager(adminId);
        accessService.requirePermission(adminId, AdminAccessService.M_CONTENT, AdminAccessService.P_EDIT);
    }

    private void requireContentDelete(UUID adminId) {
        requireContentManager(adminId);
        accessService.requirePermission(adminId, AdminAccessService.M_CONTENT, AdminAccessService.P_DELETE);
    }

    private void requireCareersRead(UUID adminId) {
        accessService.requireRead(adminId, AdminAccessService.M_CAREERS);
    }

    private void requireCareersEdit(UUID adminId) {
        accessService.requirePermission(adminId, AdminAccessService.M_CAREERS, AdminAccessService.P_EDIT);
    }

    private void requireCareersDelete(UUID adminId) {
        accessService.requirePermission(adminId, AdminAccessService.M_CAREERS, AdminAccessService.P_DELETE);
    }

    /** SUPER_ADMIN manages all countries; every other admin must be the India (IN) sub-admin. */
    private void requireContentManager(UUID adminId) {
        AdminUser admin = adminId == null
                ? null
                : adminUserRepository.findById(adminId).orElse(null);
        if (admin == null) {
            throw new AccessDeniedException("Access denied");
        }
        if (admin.getRole() != AdminRole.SUPER_ADMIN && !"IN".equalsIgnoreCase(admin.getCountryCode())) {
            throw new AccessDeniedException("Team, News and Gallery content can only be managed by the India sub-admin.");
        }
    }
}
