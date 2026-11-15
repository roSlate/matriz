package com.matriz.task;

import com.matriz.project.MembershipChanged;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to the project module's membership event (docs/architecture.md, section 5, port 2): when
 * someone is removed from a project or demoted to Viewer, their assignments in that project are
 * deleted. The tasks themselves and their comments stay (AC-03.5, AC-03.8, EC-02, EC-11). It runs
 * in the transaction of the change, so the two succeed or fail together.
 */
@Component
class MembershipChangeListener {

    private final TaskAssignmentRepository assignments;

    MembershipChangeListener(TaskAssignmentRepository assignments) {
        this.assignments = assignments;
    }

    @EventListener
    void on(MembershipChanged event) {
        assignments.deleteByProjectAndUser(event.projectId(), event.userId());
    }
}
