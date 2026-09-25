package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.PageResponse;
import com.vpnexues.svc.dto.ProductDto;
import com.vpnexues.svc.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/api/products")
    public PageResponse<ProductDto> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "SG") String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(Math.max(1, size), 200);
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(productService.list(category, search, country, pageable));
    }

    @GetMapping("/api/products/{slug}")
    public ProductDto getBySlug(@PathVariable String slug, @RequestParam(defaultValue = "SG") String country) {
        return productService.getBySlug(slug, country);
    }
}
