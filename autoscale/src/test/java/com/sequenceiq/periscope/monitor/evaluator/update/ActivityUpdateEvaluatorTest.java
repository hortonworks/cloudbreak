package com.sequenceiq.periscope.monitor.evaluator.update;


import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.context.ScalingActivitiesEvaluatorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.monitor.handler.FlowCommunicator;
import com.sequenceiq.periscope.service.ScalingActivityService;

@ExtendWith(MockitoExtension.class)
class ActivityUpdateEvaluatorTest {

    private static final Long TEST_MONITORED_ACTIVITIES_ID = 432L;

    private static final String TEST_MESSAGE = "test message";

    @Mock
    private FlowCommunicator flowCommunicator;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Captor
    private ArgumentCaptor<Collection<Long>> failedFlowCountIdsCaptor;

    @Captor
    private ArgumentCaptor<Collection<Long>> inProgressFlowCountIdsCaptor;

    @Captor
    private ArgumentCaptor<Collection<Long>> completedFlowCountIdsCaptor;

    @InjectMocks
    private ActivityUpdateEvaluator underTest;

    @Test
    void testRunThrowsExceptionAndCallsFinished() {
        ScalingActivities activities = getMonitoredScalingActivities(10);
        underTest.setContext(new ScalingActivitiesEvaluatorContext(activities));
        doThrow(RuntimeException.class).when(scalingActivityService).findAllByIds(anyCollection());

        underTest.run();

        verifyNoInteractions(flowCommunicator);
        verify(executorServiceWithRegistry).finished(underTest, TEST_MONITORED_ACTIVITIES_ID);
    }

    @Test
    void testExecuteWithScalingActivitiesNull() {
        underTest.setContext(new ScalingActivitiesEvaluatorContext(null));

        underTest.execute();

        verifyNoInteractions(flowCommunicator, scalingActivityService);
    }

    private static Stream<Arguments> dataForActivityUpdate() {
        return Stream.of(
                // Test case, FailedFlowCount, InProgressFlowCount, CompletedFlowCount
                Arguments.of("Only failed flows", 12, 0, 0),
                Arguments.of("Only failed and in progress flows", 13, 7, 0),
                Arguments.of("Failed, in progress and completed flows - 1", 17, 23, 5),
                Arguments.of("Failed, in progress and completed flows - 2", 3, 19, 29)
        );
    }

    @ParameterizedTest
    @MethodSource("dataForActivityUpdate")
    void testExecuteWithActivityIds(String testCase, int failedFlowCount, int inProgressFlowCount, int completedFlowCount) {
        int total = failedFlowCount + inProgressFlowCount + completedFlowCount;
        ScalingActivities monitoredActivites = getMonitoredScalingActivities(total);
        underTest.setContext(new ScalingActivitiesEvaluatorContext(monitoredActivites));
        doReturn(getScalingActivityEntities(total)).when(scalingActivityService).findAllByIds(anyCollection());
        doReturn(getFlowStatusMap(failedFlowCount, inProgressFlowCount, completedFlowCount)).when(flowCommunicator).getFlowStatusFromFlowIds(anyMap());
        doReturn(TEST_MESSAGE).when(messagesService).getMessage(anyString());

        underTest.execute();

        verify(scalingActivityService, times(1)).findAllByIds(monitoredActivites.getActivityIds());
        verify(scalingActivityService, times(1)).setActivityStatusAndReasonForIds(failedFlowCountIdsCaptor.capture(),
                eq(ActivityStatus.SCALING_FLOW_FAILED), anyString());
        verify(scalingActivityService, times(1)).setActivityStatusForIds(inProgressFlowCountIdsCaptor.capture(),
                eq(ActivityStatus.SCALING_FLOW_IN_PROGRESS));
        verify(scalingActivityService, times(1)).setActivityStatusForIds(completedFlowCountIdsCaptor.capture(),
                eq(ActivityStatus.SCALING_FLOW_SUCCESS));
        verify(scalingActivityService, times(2)).setEndTimes(anyMap());

        assertThat(failedFlowCountIdsCaptor.getValue()).hasSize(failedFlowCount);
        assertThat(inProgressFlowCountIdsCaptor.getValue()).hasSize(inProgressFlowCount);
        assertThat(completedFlowCountIdsCaptor.getValue()).hasSize(completedFlowCount);
    }

    private ScalingActivities getMonitoredScalingActivities(int count) {
        Set<Long> activityIds = LongStream.range(0, count).boxed().collect(toSet());
        return new ScalingActivities(TEST_MONITORED_ACTIVITIES_ID, activityIds);
    }

    private List<ScalingActivity> getScalingActivityEntities(int count) {
        List<ScalingActivity> activities = newArrayList();
        LongStream.range(0, count).forEach(i -> {
            ScalingActivity activity = new ScalingActivity();
            activity.setId(i);
            activity.setFlowId(randomUUID().toString());
            activities.add(activity);
        });
        return activities;
    }

    private Map<Long, FlowCheckResponse> getFlowStatusMap(int failedCount, int inProgressCount, int completedCount) {
        Map<Long, FlowCheckResponse> result = newHashMap();
        LongStream.range(0, failedCount).forEach(i -> {
            FlowCheckResponse response = new FlowCheckResponse();
            response.setLatestFlowFinalizedAndFailed(Boolean.TRUE);
            response.setEndTime(now().minus(Duration.of(5, ChronoUnit.MINUTES)).toEpochMilli());
            result.put(i, response);
        });

        LongStream.range(failedCount, failedCount + inProgressCount).forEach(i -> {
            FlowCheckResponse response = new FlowCheckResponse();
            response.setHasActiveFlow(Boolean.TRUE);
            result.put(i, response);
        });

        LongStream.range(failedCount + inProgressCount, failedCount + inProgressCount + completedCount).forEach(i -> {
            FlowCheckResponse response = new FlowCheckResponse();
            response.setEndTime(now().toEpochMilli());
            response.setLatestFlowFinalizedAndFailed(Boolean.FALSE);
            response.setHasActiveFlow(Boolean.FALSE);
            result.put(i, response);
        });

        return result;
    }
}