package com.matriz.task;

import com.matriz.project.TaskCounts;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The task module's implementation of the project module's {@link TaskCounts} port
 * (docs/architecture.md, section 5): the number of tasks per status, for a whole page of projects
 * in one query.
 */
@Component
class TaskCountsProvider implements TaskCounts {

    private final TaskRepository tasks;

    TaskCountsProvider(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Counts> countsFor(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, int[]> perProject = new HashMap<>(); // [todo, in progress, done]
        for (Object[] row : tasks.countByProjectAndStatus(projectIds)) {
            int[] counts = perProject.computeIfAbsent((Long) row[0], id -> new int[3]);
            counts[((TaskStatus) row[1]).ordinal()] = ((Long) row[2]).intValue();
        }

        Map<Long, Counts> result = new HashMap<>();
        perProject.forEach((id, counts) -> result.put(id, new Counts(counts[0], counts[1], counts[2])));
        return result;
    }
}
