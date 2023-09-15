package com.sequenceiq.cloudbreak.service.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.SCALE_UP;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.job.metering.MeteringJobAdapter;
import com.sequenceiq.cloudbreak.job.metering.MeteringJobService;
import com.sequenceiq.cloudbreak.metering.GrpcMeteringClient;
import com.sequenceiq.cloudbreak.service.metrics.MeteringMetricTag;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class MeteringServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String SYNC = "SYNC";

    @Mock
    private StackDtoToMeteringEventConverter stackDtoToMeteringEventConverter;

    @Mock
    private GrpcMeteringClient grpcMeteringClient;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private MeteringJobService meteringJobService;

    @Mock
    private CommonMetricService metricService;

    @InjectMocks
    private MeteringService underTest;

    @Test
    void sendMeteringSyncEventForStackShouldSendEventWhenDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToSyncEvent(any())).thenReturn(meteringEvent);
        underTest.sendMeteringSyncEventForStack(getStack(WORKLOAD, "AWS"));
        verify(grpcMeteringClient, times(1)).sendMeteringEventWithoutRetry(eq(meteringEvent));
        verify(metricService, times(0)).incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                MeteringMetricTag.REPORT_TYPE.name(), SYNC);
        verify(metricService, times(1)).incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                MeteringMetricTag.REPORT_TYPE.name(), SYNC);
    }

    @Test
    void sendMeteringStatusChangeEventForStackShouldSendEventWhenDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToStatusChangeEvent(any(), any())).thenReturn(meteringEvent);
        underTest.sendMeteringStatusChangeEventForStack(getStack(WORKLOAD, "AWS"), SCALE_UP);
        verify(grpcMeteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
        verify(metricService, times(0)).incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                MeteringMetricTag.REPORT_TYPE.name(), SCALE_UP.name());
        verify(metricService, times(1)).incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                MeteringMetricTag.REPORT_TYPE.name(), SCALE_UP.name());
    }

    @Test
    void failedSendMeteringStatusChangeEventShouldIncreaseFailureCountWhenDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToStatusChangeEvent(any(), any())).thenReturn(meteringEvent);
        doThrow(new RuntimeException("Inject Failure")).when(grpcMeteringClient).sendMeteringEvent(meteringEvent);
        underTest.sendMeteringStatusChangeEventForStack(getStack(WORKLOAD, "AWS"), SCALE_UP);
        verify(grpcMeteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
        verify(metricService, times(1)).incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                MeteringMetricTag.REPORT_TYPE.name(), SCALE_UP.name());
        verify(metricService, times(0)).incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                MeteringMetricTag.REPORT_TYPE.name(), SCALE_UP.name());
    }

    @Test
    void failedSendMeteringSyncEventShouldIncreaseFailureCountWhenDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToSyncEvent(any())).thenReturn(meteringEvent);
        doThrow(new RuntimeException("Inject Failure")).when(grpcMeteringClient).sendMeteringEventWithoutRetry(meteringEvent);
        underTest.sendMeteringSyncEventForStack(getStack(WORKLOAD, "AWS"));
        verify(grpcMeteringClient, times(1)).sendMeteringEventWithoutRetry(eq(meteringEvent));
        verify(metricService, times(1)).incrementMetricCounter(MetricType.METERING_REPORT_FAILED,
                MeteringMetricTag.REPORT_TYPE.name(), SYNC);
        verify(metricService, times(0)).incrementMetricCounter(MetricType.METERING_REPORT_SUCCESSFUL,
                MeteringMetricTag.REPORT_TYPE.name(), SYNC);
    }

    @Test
    void sendMeteringSyncEventForStackShouldNotSendEventWhenNotDatahub() {
        underTest.sendMeteringSyncEventForStack(getStack(DATALAKE, "AWS"));
        verify(stackDtoToMeteringEventConverter, never()).convertToStatusChangeEvent(any(), any());
        verify(grpcMeteringClient, never()).sendMeteringEvent(any());
    }

    @Test
    void sendMeteringSyncEventForStackShouldNotSendEventWhenDatahubButYarn() {
        underTest.sendMeteringSyncEventForStack(getStack(WORKLOAD, "YARN"));
        verify(stackDtoToMeteringEventConverter, never()).convertToStatusChangeEvent(any(), any());
        verify(grpcMeteringClient, never()).sendMeteringEvent(any());
    }

    @Test
    void sendMeteringStatusChangeEventForStackShouldNotSendEventWhenNotDatahub() {
        underTest.sendMeteringStatusChangeEventForStack(getStack(DATALAKE, "AWS"), SCALE_UP);
        verify(stackDtoToMeteringEventConverter, never()).convertToStatusChangeEvent(any(), any());
        verify(grpcMeteringClient, never()).sendMeteringEvent(any());
    }

    @Test
    void scheduleSyncShouldScheduleJobWhenDatahub() {
        StackView stack = getStack(WORKLOAD, "AWS");
        when(stackDtoService.getStackViewById(eq(STACK_ID))).thenReturn(stack);
        underTest.scheduleSync(STACK_ID);
        verify(meteringJobService, times(1)).schedule(eq(STACK_ID), eq(MeteringJobAdapter.class));
    }

    @Test
    void scheduleSyncShouldNotScheduleJobWhenNotDatahub() {
        StackView stack = getStack(DATALAKE, "AWS");
        when(stackDtoService.getStackViewById(eq(STACK_ID))).thenReturn(stack);
        underTest.scheduleSync(STACK_ID);
        verify(meteringJobService, never()).schedule(eq(STACK_ID), eq(MeteringJobAdapter.class));
    }

    @Test
    void scheduleSyncShouldNotScheduleJobWhenDatahubButYarn() {
        StackView stack = getStack(WORKLOAD, "YARN");
        when(stackDtoService.getStackViewById(eq(STACK_ID))).thenReturn(stack);
        underTest.scheduleSync(STACK_ID);
        verify(meteringJobService, never()).schedule(eq(STACK_ID), eq(MeteringJobAdapter.class));
    }

    @Test
    void unscheduleSyncShouldScheduleJobWhenDatahub() {
        StackView stack = getStack(WORKLOAD, "AWS");
        when(stackDtoService.getStackViewById(eq(STACK_ID))).thenReturn(stack);
        underTest.unscheduleSync(STACK_ID);
        verify(meteringJobService, times(1)).unschedule(eq(String.valueOf(STACK_ID)));
    }

    @Test
    void unscheduleSyncShouldNotScheduleJobWhenNotDatahub() {
        StackView stack = getStack(DATALAKE, "AWS");
        when(stackDtoService.getStackViewById(eq(STACK_ID))).thenReturn(stack);
        underTest.unscheduleSync(STACK_ID);
        verify(meteringJobService, never()).unschedule(eq(String.valueOf(STACK_ID)));
    }

    @Test
    void unscheduleSyncShouldNotScheduleJobWhenDatahubButYarn() {
        StackView stack = getStack(DATALAKE, "YARN");
        when(stackDtoService.getStackViewById(eq(STACK_ID))).thenReturn(stack);
        underTest.unscheduleSync(STACK_ID);
        verify(meteringJobService, never()).unschedule(eq(String.valueOf(STACK_ID)));
    }

    private Stack getStack(StackType stackType, String cloudPlatform) {
        Stack stack = new Stack();
        stack.setType(stackType);
        stack.setCloudPlatform(cloudPlatform);
        return stack;
    }
}