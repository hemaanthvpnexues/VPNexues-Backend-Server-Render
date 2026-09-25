package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CategoryDto;
import com.vpnexues.svc.dto.CreateCategoryRequest;
import com.vpnexues.svc.dto.UpdateCategoryRequest;
import com.vpnexues.svc.entity.Category;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.CategoryRepository;
import com.vpnexues.svc.util.Slugify;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public CategoryDto create(CreateCategoryRequest req) {
        if (categoryRepository.existsByName(req.name())) {
            throw new BadRequestException("A category named '" + req.name() + "' already exists");
        }
        String slug = StringUtils.hasText(req.slug()) ? Slugify.slugify(req.slug()) : Slugify.slugify(req.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("A category with slug '" + slug + "' already exists");
        }

        Category category = new Category();
        category.setName(req.name());
        category.setSlug(slug);
        category.setEmoji(req.emoji());
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto update(UUID id, UpdateCategoryRequest req) {
        Category category =
                categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found: " + id));
        if (categoryRepository.existsByNameAndIdNot(req.name(), id)) {
            throw new BadRequestException("A category named '" + req.name() + "' already exists");
        }

        category.setName(req.name());
        category.setEmoji(req.emoji());
        category.setActive(req.active());
        return toDto(categoryRepository.save(category));
    }

    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getEmoji(), c.isActive());
    }
}
