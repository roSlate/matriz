package com.matriz.project;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProgressTest {

    private static Progress progress(int todo, int inProgress, int done, Integer manual) {
        return Progress.of(new TaskCounts.Counts(todo, inProgress, done), manual);
    }

    @Test
    void aProjectWithoutTasksIsAtZeroPercent() {
        assertThat(progress(0, 0, 0, null).computed()).isZero();
    }

    @Test
    void isTheShareOfDoneTasksRoundedToTheNearestWholeNumber() {
        assertThat(progress(2, 0, 1, null).computed()).isEqualTo(33);   // 1 of 3 = 33.33
        assertThat(progress(1, 0, 2, null).computed()).isEqualTo(67);   // 2 of 3 = 66.67
        assertThat(progress(4, 2, 6, null).computed()).isEqualTo(50);   // 6 of 12
        assertThat(progress(0, 0, 5, null).computed()).isEqualTo(100);
    }

    @Test
    void halvesAreRoundedUp() {
        assertThat(progress(7, 0, 1, null).computed()).isEqualTo(13);   // 1 of 8 = 12.5
        assertThat(progress(1, 0, 7, null).computed()).isEqualTo(88);   // 7 of 8 = 87.5
        assertThat(progress(39, 0, 1, null).computed()).isEqualTo(3);   // 1 of 40 = 2.5
    }

    @Test
    void theManualValueIsShownInsteadOfTheComputedOneButTheComputedOneStaysAvailable() {
        Progress progress = progress(3, 1, 2, 80);

        assertThat(progress.manual()).isEqualTo(80);
        assertThat(progress.computed()).isEqualTo(33);
        assertThat(progress.effective()).isEqualTo(80);
    }

    @Test
    void withoutAManualValueTheEffectiveValueIsTheComputedOne() {
        Progress progress = progress(1, 0, 1, null);

        assertThat(progress.manual()).isNull();
        assertThat(progress.effective()).isEqualTo(progress.computed());
    }

    @Test
    void aManualZeroIsStillAManualValue() {
        assertThat(progress(0, 0, 4, 0).effective()).isZero();
    }
}
