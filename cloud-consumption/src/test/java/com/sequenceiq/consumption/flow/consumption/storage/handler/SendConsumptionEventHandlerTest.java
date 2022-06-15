package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.SEND_CONSUMPTION_EVENT_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.usage.MeteringEventProcessor;
import com.sequenceiq.consumption.converter.metering.ConsumptionToStorageHeartbeatConverter;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;
import com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionFlowException;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class SendConsumptionEventHandlerTest {

    private static final String CRN = "crn";

    private static final Long ID = 42L;

    @Mock
    private MeteringEventProcessor meteringEventProcessor;

    @Mock
    private ConsumptionToStorageHeartbeatConverter storageHeartbeatConverter;

    @Mock
    private Consumption consumption;

    @Mock
    private StorageConsumptionResult storageResult;

    @InjectMocks
    private SendConsumptionEventHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(SEND_CONSUMPTION_EVENT_HANDLER.selector(), underTest.selector());
    }

    @Test
    public void testOperationName() {
        assertEquals("Send storage consumption event", underTest.getOperationName());
    }

    @Test
    public void testDoAcceptThrowsExceptionWhenStorageIsNull() {
        StorageConsumptionCollectionHandlerEvent event = new StorageConsumptionCollectionHandlerEvent(
                SEND_CONSUMPTION_EVENT_HANDLER.selector(),
                ID, CRN, new ConsumptionContext(null, consumption), null);

        StorageConsumptionCollectionFlowException ex = assertThrows(StorageConsumptionCollectionFlowException.class,
                () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertThat(ex.getCause(), instanceOf(OperationException.class));
        assertEquals("StorageConsumptionResult missing from StorageConsumptionCollectionHandlerEvent, " +
                "cannot send StorageHeartbeat for Consumption with CRN [crn].", ex.getCause().getMessage());
    }

    @Test
    public void testDoAccept() {
        StorageConsumptionCollectionHandlerEvent event = new StorageConsumptionCollectionHandlerEvent(
                SEND_CONSUMPTION_EVENT_HANDLER.selector(),
                ID, CRN, new ConsumptionContext(null, consumption), storageResult);

        MeteringEventsProto.StorageHeartbeat heartbeat = MeteringEventsProto.StorageHeartbeat.newBuilder().build();
        when(storageHeartbeatConverter.convertToS3StorageHeartBeat(consumption, storageResult)).thenReturn(heartbeat);
        doNothing().when(meteringEventProcessor).storageHeartbeat(heartbeat, MeteringEventsProto.ServiceType.Value.ENVIRONMENT);

        StorageConsumptionCollectionEvent result = (StorageConsumptionCollectionEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(storageHeartbeatConverter).convertToS3StorageHeartBeat(consumption, storageResult);
        verify(meteringEventProcessor).storageHeartbeat(heartbeat, MeteringEventsProto.ServiceType.Value.ENVIRONMENT);
        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT.selector(), result.selector());
    }
}
