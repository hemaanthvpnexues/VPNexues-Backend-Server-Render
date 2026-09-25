package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.StoreSettingsDto;
import com.vpnexues.svc.dto.UpdateStoreSettingsRequest;
import com.vpnexues.svc.entity.StoreSettings;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;

    @Transactional(readOnly = true)
    public StoreSettingsDto get() {
        return toDto(getEntity());
    }

    public StoreSettingsDto update(UpdateStoreSettingsRequest req) {
        StoreSettings settings = getEntity();
        settings.setWebsiteName(req.websiteName());
        settings.setTagline(req.tagline());
        settings.setDefaultCurrency(req.defaultCurrency());
        settings.setTimezone(req.timezone());
        settings.setDateFormat(req.dateFormat());
        settings.setTimeFormat(req.timeFormat());
        settings.setWebsiteEmail(req.websiteEmail());
        settings.setWebsitePhone(req.websitePhone());
        settings.setStoreName(req.storeName());
        settings.setStoreEmail(req.storeEmail());
        settings.setStorePhone(req.storePhone());
        settings.setStoreAddress(req.storeAddress());
        settings.setStoreDescription(req.storeDescription());
        settings.setFreeShippingThreshold(req.freeShippingThreshold());
        settings.setStandardShippingRate(req.standardShippingRate());
        settings.setExpressShippingRate(req.expressShippingRate());
        settings.setDefaultDeliveryDays(req.defaultDeliveryDays());
        settings.setFacebookUrl(req.facebookUrl());
        settings.setInstagramUrl(req.instagramUrl());
        settings.setTwitterUrl(req.twitterUrl());
        settings.setYoutubeUrl(req.youtubeUrl());
        settings.setWhatsappUrl(req.whatsappUrl());
        settings.setTelegramUrl(req.telegramUrl());
        settings.setSmallBoxStandardSlots(req.smallBoxStandardSlots());
        settings.setSmallBoxPremiumRate(req.smallBoxPremiumRate());
        settings.setBigBoxMinKg(req.bigBoxMinKg());
        settings.setBigBoxDiscountRate(req.bigBoxDiscountRate());
        return toDto(storeSettingsRepository.save(settings));
    }

    private StoreSettings getEntity() {
        return storeSettingsRepository
                .findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException(
                        "store_settings has no row — R__Seed_data.sql should have seeded exactly one"));
    }

    private StoreSettingsDto toDto(StoreSettings s) {
        return new StoreSettingsDto(
                s.getWebsiteName(),
                s.getTagline(),
                s.getDefaultCurrency(),
                s.getTimezone(),
                s.getDateFormat(),
                s.getTimeFormat(),
                s.getWebsiteEmail(),
                s.getWebsitePhone(),
                s.getStoreName(),
                s.getStoreEmail(),
                s.getStorePhone(),
                s.getStoreAddress(),
                s.getStoreDescription(),
                s.getFreeShippingThreshold(),
                s.getStandardShippingRate(),
                s.getExpressShippingRate(),
                s.getDefaultDeliveryDays(),
                s.getFacebookUrl(),
                s.getInstagramUrl(),
                s.getTwitterUrl(),
                s.getYoutubeUrl(),
                s.getWhatsappUrl(),
                s.getTelegramUrl(),
                s.getSmallBoxStandardSlots(),
                s.getSmallBoxPremiumRate(),
                s.getBigBoxMinKg(),
                s.getBigBoxDiscountRate());
    }
}
