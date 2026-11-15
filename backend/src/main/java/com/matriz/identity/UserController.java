package com.matriz.identity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The authenticated user (FR-03). */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /** Returns who the session belongs to. The identity comes from the session, never from the request. */
    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.of(principal);
    }
}
