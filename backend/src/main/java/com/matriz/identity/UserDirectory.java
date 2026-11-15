package com.matriz.identity;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What other modules may ask the identity module about users. They get names and emails, never
 * entities or password hashes, and they do not touch the users table themselves.
 */
@Service
public class UserDirectory {

    private final UserRepository users;

    public UserDirectory(UserRepository users) {
        this.users = users;
    }

    /** The summaries of the given users, keyed by id, in one query. Unknown ids are left out. */
    @Transactional(readOnly = true)
    public Map<Long, UserSummary> summaries(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(ids).stream()
                .map(user -> new UserSummary(user.getId(), user.getName(), user.getEmail()))
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }
}
