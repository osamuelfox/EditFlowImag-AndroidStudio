package com.editflowimg.backend.controller;

import com.editflowimg.backend.dto.auth.UserResponse;
import com.editflowimg.backend.entity.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserEntity user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
