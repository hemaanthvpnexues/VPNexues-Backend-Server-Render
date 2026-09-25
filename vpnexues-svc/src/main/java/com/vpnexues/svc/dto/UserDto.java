package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.User;
import java.util.UUID;

public record UserDto(UUID id, String name, String email, String phone, boolean emailVerified) {

    public static UserDto of(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.isEmailVerified());
    }
}
