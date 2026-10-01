package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.ProductPriceOverride;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceOverrideRepository extends JpaRepository<ProductPriceOverride, UUID> {

    Optional<ProductPriceOverride> findByProductIdAndCountryCode(UUID productId, String countryCode);

    List<ProductPriceOverride> findByProductIdIn(List<UUID> productIds);

    List<ProductPriceOverride> findByCountryCodeAndProductIdIn(String countryCode, List<UUID> productIds);
}
