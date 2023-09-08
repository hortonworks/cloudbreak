package com.sequenceiq.cloudbreak.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;

import io.grpc.ManagedChannel;

@ExtendWith(MockitoExtension.class)
class GrpcMeteringClientTest {

    @Mock
    private ManagedChannelWrapper channelWrapper;

    @Mock
    private MeteringConfig meteringConfig;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private MeteringInfoProvider meteringInfoProvider;

    @Mock
    private ManagedChannel managedChannel;

    @Mock
    private MeteringClient meteringClient;

    @InjectMocks
    private GrpcMeteringClient rawGrpcMeteringClient;

    private GrpcMeteringClient underTest;

    @BeforeEach
    public void setUp() {
        underTest = spy(rawGrpcMeteringClient);
        lenient().doReturn(managedChannel).when(channelWrapper).getChannel();
        lenient().doReturn(meteringClient).when(underTest).makeClient();
    }

    @Test
    void testSendMeteringEvent() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        underTest.sendMeteringEvent(meteringEvent);
        verify(meteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
    }

    @Test
    void testSendMeteringEventWithoutRetry() {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        MeteringEvent meteringEvent = MeteringEvent.newBuilder().build();
        underTest.sendMeteringEvent(meteringEvent);
        verify(meteringClient, times(1)).sendMeteringEvent(eq(meteringEvent));
    }
}