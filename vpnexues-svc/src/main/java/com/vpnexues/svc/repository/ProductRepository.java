package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    // Bestseller: salesCount DESC (tomato first, onion second...) — applies to Shop / Small Box / Big Box
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.salesCount DESC, p.name ASC")
    Page<Product> findByActiveTrueOrderBySalesCountDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.salesCountSg DESC, p.name ASC")
    Page<Product> findByActiveTrueOrderBySalesCountSgDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.salesCountUs DESC, p.name ASC")
    Page<Product> findByActiveTrueOrderBySalesCountUsDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.salesCountAe DESC, p.name ASC")
    Page<Product> findByActiveTrueOrderBySalesCountAeDesc(Pageable pageable);

    Page<Product> findByActiveTrue(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p LEFT JOIN FETCH p.badges WHERE p.id IN :ids")
    List<Product> findWithBadgesByIdIn(@org.springframework.data.repository.query.Param("ids") java.util.Collection<UUID> ids);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category = :category ORDER BY p.salesCount DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryOrderBySalesCountDesc(@org.springframework.data.repository.query.Param("category") String category, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category = :category ORDER BY p.salesCountSg DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryOrderBySalesCountSgDesc(@org.springframework.data.repository.query.Param("category") String category, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category = :category ORDER BY p.salesCountUs DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryOrderBySalesCountUsDesc(@org.springframework.data.repository.query.Param("category") String category, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category = :category ORDER BY p.salesCountAe DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryOrderBySalesCountAeDesc(@org.springframework.data.repository.query.Param("category") String category, Pageable pageable);

    Page<Product> findByActiveTrueAndCategory(String category, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.salesCount DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountDesc(@org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.salesCountSg DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountSgDesc(@org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.salesCountUs DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountUsDesc(@org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.salesCountAe DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountAeDesc(@org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String query, Pageable pageable);

    // TEMP exclusion variants (beverages hidden per head, 07-Oct-2026) — storefront
    // listing minus a set of categories; admin variants below stay unfiltered.
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category NOT IN :categories ORDER BY p.salesCount DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryNotInOrderBySalesCountDesc(@org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category NOT IN :categories ORDER BY p.salesCountSg DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryNotInOrderBySalesCountSgDesc(@org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category NOT IN :categories ORDER BY p.salesCountUs DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryNotInOrderBySalesCountUsDesc(@org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND p.category NOT IN :categories ORDER BY p.salesCountAe DESC, p.name ASC")
    Page<Product> findByActiveTrueAndCategoryNotInOrderBySalesCountAeDesc(@org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.category NOT IN :categories ORDER BY p.salesCount DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseAndCategoryNotInOrderBySalesCountDesc(@org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.category NOT IN :categories ORDER BY p.salesCountSg DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseAndCategoryNotInOrderBySalesCountSgDesc(@org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.category NOT IN :categories ORDER BY p.salesCountUs DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseAndCategoryNotInOrderBySalesCountUsDesc(@org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.category NOT IN :categories ORDER BY p.salesCountAe DESC, p.name ASC")
    Page<Product> findByActiveTrueAndNameContainingIgnoreCaseAndCategoryNotInOrderBySalesCountAeDesc(@org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("categories") java.util.Collection<String> categories, Pageable pageable);

    // Admin variants — deliberately not activeTrue-scoped, so inactive products stay
    // manageable in the admin UI instead of disappearing once deactivated.
    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String query, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    long countByActiveTrue();
}
