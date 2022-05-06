package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.SEND_CONSUMPTION_EVENT_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

public class SendConsumptionEventHandlerTest {

    private SendConsumptionEventHandler underTest;

    @BeforeEach
    public void setUp() {
        underTest = new SendConsumptionEventHandler();
    }

    @Test
    public void testSelector() {
        assertEquals(SEND_CONSUMPTION_EVENT_HANDLER.selector(), underTest.selector());
    }

    @Test
    public void testOperationName() {
        assertEquals("Send storage consumption event", underTest.getOperationName());
    }

    @Test
    public void testExecuteOperation() {
        String resourceCrn = "consumptionCrn";
        Long resourceId = 1L;

        StorageConsumptionCollectionHandlerEvent event = new StorageConsumptionCollectionHandlerEvent(
                SEND_CONSUMPTION_EVENT_HANDLER.selector(),
                resourceId, resourceCrn, null);

        StorageConsumptionCollectionEvent result = (StorageConsumptionCollectionEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(resourceCrn, result.getResourceCrn());
        assertEquals(resourceId, result.getResourceId());
        assertEquals(STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT.selector(), result.selector());
    }
}
