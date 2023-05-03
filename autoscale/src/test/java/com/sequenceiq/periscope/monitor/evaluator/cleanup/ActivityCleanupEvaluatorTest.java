package com.sequenceiq.periscope.monitor.evaluator.cleanup;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.context.ScalingActivitiesEvaluatorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.service.ScalingActivityService;

@ExtendWith(MockitoExtension.class)
class ActivityCleanupEvaluatorTest {

    private static final Long TEST_SCALING_ACTIVITIES_ID = 1L;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Captor
    private ArgumentCaptor<Set<Long>> captor;

    @InjectMocks
    private ActivityCleanupEvaluator underTest;

    @Test
    void testRunCallsFinishedWhenException() {
        underTest.setContext(new ScalingActivitiesEvaluatorContext(getScalingActivities(10)));
        doThrow(new RuntimeException("test exception")).when(scalingActivityService).deleteScalingActivityByIds(anySet());

        underTest.run();

        verify(executorServiceWithRegistry).finished(underTest, TEST_SCALING_ACTIVITIES_ID);
    }

    @Test
    void testExecuteWhenScalingActivitiesAreNull() {
        underTest.setContext(new ScalingActivitiesEvaluatorContext(null));

        underTest.execute();

        verifyNoInteractions(scalingActivityService);
    }

    @Test
    void testExecuteWhenScalingActivityIdsAreEmpty() {
        underTest.setContext(new ScalingActivitiesEvaluatorContext(getScalingActivities(0)));

        underTest.execute();

        verify(scalingActivityService, never()).deleteScalingActivityByIds(anySet());
    }

    @Test
    void testExecuteWithScalingActivities() {
        ScalingActivities activities = getScalingActivities(10);
        underTest.setContext(new ScalingActivitiesEvaluatorContext(activities));

        underTest.execute();

        verify(scalingActivityService, times(1)).deleteScalingActivityByIds(captor.capture());
        Set<Long> idsToDelete = captor.getValue();
        assertThat(idsToDelete).hasSameElementsAs(activities.getActivityIds());
    }

    private ScalingActivities getScalingActivities(int count) {
        ScalingActivities scalingActivities = new ScalingActivities();
        scalingActivities.setId(TEST_SCALING_ACTIVITIES_ID);
        scalingActivities.setActivityIds(LongStream.range(1, count).boxed().collect(toSet()));
        return scalingActivities;
    }
}