package com.matriz.identity;

/** A user as returned by the API. It never contains the password or its hash. */
public record UserResponse(Long id, String name, String email) {

    static UserResponse of(UserPrincipal principal) {
        return new UserResponse(principal.id(), principal.name(), principal.email());
    }
}
