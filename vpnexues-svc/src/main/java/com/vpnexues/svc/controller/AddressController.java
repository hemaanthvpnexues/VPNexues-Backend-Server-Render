package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AddressDto;
import com.vpnexues.svc.dto.CreateAddressRequest;
import com.vpnexues.svc.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/api/addresses")
    public List<AddressDto> list(@AuthenticationPrincipal UUID userId) {
        return addressService.list(userId);
    }

    @PostMapping("/api/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto create(@AuthenticationPrincipal UUID userId, @RequestBody @Valid CreateAddressRequest req) {
        return addressService.create(userId, req);
    }

    @DeleteMapping("/api/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        addressService.delete(userId, id);
    }
}
