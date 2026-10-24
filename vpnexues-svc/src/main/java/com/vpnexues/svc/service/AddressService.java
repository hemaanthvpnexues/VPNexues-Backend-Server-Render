package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AddressDto;
import com.vpnexues.svc.dto.CreateAddressRequest;
import com.vpnexues.svc.entity.Address;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AddressRepository;
import com.vpnexues.svc.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressDto> list(UUID userId) {
        return addressRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    public AddressDto create(UUID userId, CreateAddressRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        if (req.isDefault()) {
            addressRepository.findByUserId(userId).forEach(a -> {
                a.setDefault(false);
                addressRepository.save(a);
            });
        }

        Address address = new Address();
        address.setUser(user);
        address.setLabel(req.label());
        address.setName(req.name());
        address.setPhone(req.phone());
        address.setAddressLine(req.addressLine());
        address.setFlat(req.flat());
        address.setLandmark(req.landmark());
        address.setCity(req.city() != null ? req.city() : "");
        address.setPincode(req.pincode() != null ? req.pincode() : "");
        address.setState(req.state() != null ? req.state() : "");
        // V59: remember which country this address is in, so checkout validates the
        // city against the address's own country rather than the header selector
        // (which was Singapore in the Tirupattur case and wildcard-passed it).
        address.setCountry(normalizeCountry(req.country()));
        address.setDefault(req.isDefault());
        address.setLat(req.lat());
        address.setLng(req.lng());
        return toDto(addressRepository.save(address));
    }

    Address getEntityForUser(UUID addressId, UUID userId) {
        return addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));
    }

    /**
     * Blank / over-long / junk country input collapses to null rather than
     * tripping the VARCHAR(2) limit. Checkout treats null as "unknown" and falls
     * back to the selected country, so a bad value degrades to the old behaviour
     * instead of failing the save.
     */
    private String normalizeCountry(String country) {
        if (country == null) return null;
        String trimmed = country.trim().toUpperCase(java.util.Locale.ROOT);
        if (trimmed.length() != 2 || !trimmed.chars().allMatch(c -> c >= 'A' && c <= 'Z')) {
            return null;
        }
        return trimmed;
    }

    public void delete(UUID userId, UUID addressId) {
        Address address = getEntityForUser(addressId, userId);
        addressRepository.delete(address);
    }

    private AddressDto toDto(Address a) {
        return new AddressDto(
                a.getId(),
                a.getLabel(),
                a.getName(),
                a.getPhone(),
                a.getAddressLine(),
                a.getFlat(),
                a.getLandmark(),
                a.getCity(),
                a.getPincode(),
                a.getState(),
                a.getCountry(),
                a.isDefault(),
                a.getLat(),
                a.getLng());
    }
}
