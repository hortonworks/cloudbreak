package com.sequenceiq.cloudbreak.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.usage.metering.MeteringDatabusRecordProcessor;

@ExtendWith(MockitoExtension.class)
public class MeteringEventProcessorTest {

    @Mock
    private MeteringDatabusRecordProcessor meteringDatabusRecordProcessor;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private MeteringEventProcessor underTest;

    @Test
    public void testStorageHeartbeatWithNull() {
        assertThrows(NullPointerException.class, () -> underTest.storageHeartbeat(null,
                MeteringEventsProto.ServiceType.Value.ENVIRONMENT,
                MeteringEventsProto.ServiceFeature.Value.OBJECT_STORAGE));
        assertThrows(NullPointerException.class, () -> underTest.storageHeartbeat(MeteringEventsProto.StorageHeartbeat.newBuilder().build(), null, null));
    }

    @Test
    public void testStorageHeartbeat() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(internalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        MeteringEventsProto.StorageHeartbeat storageHeartbeat = MeteringEventsProto.StorageHeartbeat.newBuilder().build();

        underTest.storageHeartbeat(storageHeartbeat, MeteringEventsProto.ServiceType.Value.ENVIRONMENT, MeteringEventsProto.ServiceFeature.Value.OBJECT_STORAGE);

        ArgumentCaptor<DatabusRequest> captor = ArgumentCaptor.forClass(DatabusRequest.class);
        verify(meteringDatabusRecordProcessor).processRecord(captor.capture());

        assertEquals("altus", captor.getValue().getContext().get().getAccountId());

        MeteringEventsProto.MeteringEvent event = (MeteringEventsProto.MeteringEvent) captor.getValue().getMessageBody().get();
        assertEquals(3, event.getVersion());
        assertEquals(storageHeartbeat, event.getStorageHeartbeat());
        assertEquals(MeteringEventsProto.ServiceType.Value.ENVIRONMENT, event.getServiceType());
        assertEquals(MeteringEventsProto.ServiceFeature.Value.OBJECT_STORAGE, event.getServiceConfiguration().getServiceFeature());
    }
}
