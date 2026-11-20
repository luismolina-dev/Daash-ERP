package com.daash.erp.dto;

import com.daash.erp.model.User;

public record UserDto(Long id, String nombre, String email, String role) {
    public static UserDto from(User user) {
        return new UserDto(user.id, user.nombre, user.email, user.role.value());
    }
}
