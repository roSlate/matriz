package com.matriz.project;

import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Stand-in for {@link TaskCounts} until the task module exists (increment 4); it is removed then. */
@Component
class NoTasksYet implements TaskCounts {

    @Override
    public Map<Long, Counts> countsFor(Collection<Long> projectIds) {
        return Map.of();
    }
}
