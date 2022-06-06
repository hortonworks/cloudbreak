package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.consumption.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.Credential;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;
import com.sequenceiq.consumption.flow.consumption.storage.event.SendStorageConsumptionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.consumption.service.CredentialService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class StorageConsumptionCollectionHandlerTest {

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @InjectMocks
    private StorageConsumptionCollectionHandler underTest;

    @Mock
    private Credential credential;

    @Test
    public void testSelector() {
        assertEquals(STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector(), underTest.selector());
    }

    @Test
    public void testOperationName() {
        assertEquals("Collect storage consumption data", underTest.getOperationName());
    }

    @Test
    public void testExecuteOperation() {
        String resourceCrn = "consumptionCrn";
        Long resourceId = 1L;
        String envCrn = "envCrn";

        Consumption consumption = new Consumption();
        consumption.setResourceCrn(resourceCrn);
        consumption.setId(resourceId);
        consumption.setEnvironmentCrn(envCrn);

        ConsumptionContext context = new ConsumptionContext(null, consumption);
        StorageConsumptionCollectionHandlerEvent event = new StorageConsumptionCollectionHandlerEvent(
                STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector(),
                resourceId, resourceCrn, context, null);

        when(credentialService.getCredentialByEnvCrn(envCrn)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(new CloudCredential());

        SendStorageConsumptionEvent result = (SendStorageConsumptionEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(credentialService).getCredentialByEnvCrn(envCrn);
        verify(credentialConverter).convert(credential);

        assertEquals(resourceCrn, result.getResourceCrn());
        assertEquals(resourceId, result.getResourceId());
        assertNull(result.getStorageConsumptionResult());
        assertEquals(SEND_CONSUMPTION_EVENT_EVENT.selector(), result.selector());
    }
}
