package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.CreateUserRequest;
import com.devbridge.logicore.dto.UpdateUserRequest;
import com.devbridge.logicore.dto.UserAdminResponse;
import com.devbridge.logicore.security.AuthenticatedUser;
import com.devbridge.logicore.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserAdminResponse> list() {
        return userService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAdminResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    public UserAdminResponse update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateUserRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return userService.update(id, request, actor.getId());
    }
}