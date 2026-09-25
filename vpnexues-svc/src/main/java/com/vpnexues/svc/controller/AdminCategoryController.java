package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CategoryDto;
import com.vpnexues.svc.dto.CreateCategoryRequest;
import com.vpnexues.svc.dto.UpdateCategoryRequest;
import com.vpnexues.svc.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/admin/categories")
    public List<CategoryDto> list() {
        return categoryService.list();
    }

    @PostMapping("/api/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@RequestBody @Valid CreateCategoryRequest req) {
        return categoryService.create(req);
    }

    @PutMapping("/api/admin/categories/{id}")
    public CategoryDto update(@PathVariable UUID id, @RequestBody @Valid UpdateCategoryRequest req) {
        return categoryService.update(id, req);
    }

    @DeleteMapping("/api/admin/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
