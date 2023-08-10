package com.sequenceiq.cloudbreak.service.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.SCALE_UP;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.metering.GrpcMeteringClient;

@ExtendWith(MockitoExtension.class)
class MeteringServiceTest {

    @Mock
    private StackDtoToMeteringEventConverter stackDtoToMeteringEventConverter;

    @Mock
    private GrpcMeteringClient grpcMeteringClient;

    @InjectMocks
    private MeteringService underTest;

    @Test
    void sendMeteringSyncEventForStackShouldSendEventWhenDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToSyncEvent(any())).thenReturn(meteringEvent);
        underTest.sendMeteringSyncEventForStack(getStack(WORKLOAD));
        verify(grpcMeteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
    }

    @Test
    void sendMeteringStatusChangeEventForStackShouldSendEventWhenDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToStatusChangeEvent(any(), any())).thenReturn(meteringEvent);
        underTest.sendMeteringStatusChangeEventForStack(getStack(WORKLOAD), SCALE_UP);
        verify(grpcMeteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
    }

    @Test
    void sendMeteringSyncEventForStackShouldNotSendEventWhenNotDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        underTest.sendMeteringSyncEventForStack(getStack(DATALAKE));
        verify(stackDtoToMeteringEventConverter, never()).convertToStatusChangeEvent(any(), any());
        verify(grpcMeteringClient, never()).sendMeteringEvent(any());
    }

    @Test
    void sendMeteringStatusChangeEventForStackShouldNotSendEventWhenNotDatahub() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        underTest.sendMeteringStatusChangeEventForStack(getStack(DATALAKE), SCALE_UP);
        verify(stackDtoToMeteringEventConverter, never()).convertToStatusChangeEvent(any(), any());
        verify(grpcMeteringClient, never()).sendMeteringEvent(any());
    }

    private Stack getStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        return stack;
    }
}