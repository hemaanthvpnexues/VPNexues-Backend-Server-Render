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
                a.isDefault(),
                a.getLat(),
                a.getLng());
    }
}
