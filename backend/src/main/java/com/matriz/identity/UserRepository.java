package com.matriz.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for {@link User}. Used by the identity module only. */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Finds a user by email, ignoring letter case (assumption A-23). */
    Optional<User> findByEmailIgnoreCase(String email);
}
