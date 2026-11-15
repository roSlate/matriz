package com.matriz.project;

import java.util.Collection;
import java.util.Map;

/**
 * Port: how many tasks a project has in each status. The project module needs this for the
 * dashboard and for progress, but it must not depend on the task module (docs/architecture.md,
 * section 5), so it defines the interface and the task module implements it.
 */
public interface TaskCounts {

    /** Counts for several projects at once, so a page of projects needs one query. A missing project has none. */
    Map<Long, Counts> countsFor(Collection<Long> projectIds);

    /** Number of tasks per status. */
    record Counts(int todo, int inProgress, int done) {

        public static final Counts NONE = new Counts(0, 0, 0);

        public int total() {
            return todo + inProgress + done;
        }
    }
}
