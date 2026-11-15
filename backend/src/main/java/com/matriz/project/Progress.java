package com.matriz.project;

/**
 * Progress of a project (FR-24, FR-25): the share of tasks that are done, as a whole percentage,
 * unless the Owner has set a value by hand.
 *
 * @param computed from the tasks: done divided by all, rounded to the nearest whole number with
 *                 halves rounded up; 0 when there are no tasks
 * @param manual   the Owner's value (0 to 100), or null when there is none
 * @param effective the value to show: {@code manual} when set, otherwise {@code computed}
 */
public record Progress(int computed, Integer manual, int effective) {

    public static Progress of(TaskCounts.Counts counts, Integer manual) {
        int computed = computedPercentage(counts);
        return new Progress(computed, manual, manual != null ? manual : computed);
    }

    static int computedPercentage(TaskCounts.Counts counts) {
        int total = counts.total();
        if (total == 0) {
            return 0;
        }
        // (done * 100) / total rounded half up, using whole numbers only: no floating point surprises.
        return (2 * counts.done() * 100 + total) / (2 * total);
    }
}
