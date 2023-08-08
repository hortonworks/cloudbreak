package com.sequenceiq.cloudbreak.service.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.SCALE_UP;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.converter.StackDtoToMeteringEventConverter;
import com.sequenceiq.cloudbreak.dto.StackDto;
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
    void testSendMeteringSyncEventForStack() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToSyncEvent(any())).thenReturn(meteringEvent);
        underTest.sendMeteringSyncEventForStack(new StackDto());
        verify(grpcMeteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
    }

    @Test
    void testSendMeteringStatusChangeEventForStack() {
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        when(stackDtoToMeteringEventConverter.convertToStatusChangeEvent(any(), any())).thenReturn(meteringEvent);
        underTest.sendMeteringStatusChangeEventForStack(new StackDto(), SCALE_UP);
        verify(grpcMeteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
    }
}