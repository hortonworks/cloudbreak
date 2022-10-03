package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.SEND_CONSUMPTION_EVENT_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.consumption.AwsS3ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.usage.MeteringEventProcessor;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;
import com.sequenceiq.consumption.flow.consumption.storage.StorageConsumptionCollectionFlowException;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.consumption.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class SendConsumptionEventHandlerTest {

    private static final String CRN = "crn";

    private static final Long ID = 42L;

    private static final String VALID_STORAGE_LOCATION = "s3a://bucket-name/folder/file";

    private static final String ENV_CRN = "env-crn";

    private static final double STORAGE_IN_BYTES = 1.0;

    @Mock
    private MeteringEventProcessor meteringEventProcessor;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private AwsS3ConsumptionCalculator awsS3ConsumptionCalculator;

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
        StorageConsumptionCollectionHandlerEvent event = createInputEvent(CloudPlatform.AWS.name(), null);
        StorageConsumptionCollectionFlowException ex = assertThrows(StorageConsumptionCollectionFlowException.class,
                () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertThat(ex.getCause(), instanceOf(OperationException.class));
        assertEquals("StorageConsumptionResult missing from StorageConsumptionCollectionHandlerEvent, " +
                "cannot send StorageHeartbeat for Consumption with CRN [crn].", ex.getCause().getMessage());
    }

    @Test
    public void testDoAccept() {
        StorageConsumptionResult storageResult = new StorageConsumptionResult(STORAGE_IN_BYTES);
        StorageConsumptionCollectionHandlerEvent event = createInputEvent(CloudPlatform.AWS.name(), storageResult);
        MeteringEventsProto.StorageHeartbeat heartbeat = MeteringEventsProto.StorageHeartbeat.newBuilder().build();

        when(awsS3ConsumptionCalculator.convertToStorageHeartbeat(any(CloudConsumption.class), eq(storageResult.getStorageInBytes()))).thenReturn(heartbeat);
        when(awsS3ConsumptionCalculator.getMeteringServiceType()).thenReturn(MeteringEventsProto.ServiceType.Value.ENVIRONMENT);
        when(awsS3ConsumptionCalculator.getServiceFeature()).thenReturn(MeteringEventsProto.ServiceFeature.Value.OBJECT_STORAGE);
        doNothing().when(meteringEventProcessor).storageHeartbeat(heartbeat,
                MeteringEventsProto.ServiceType.Value.ENVIRONMENT,
                MeteringEventsProto.ServiceFeature.Value.OBJECT_STORAGE);
        StorageConsumptionCollectionEvent result = (StorageConsumptionCollectionEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(awsS3ConsumptionCalculator).convertToStorageHeartbeat(any(CloudConsumption.class), eq(storageResult.getStorageInBytes()));
        verify(meteringEventProcessor).storageHeartbeat(heartbeat, MeteringEventsProto.ServiceType.Value.ENVIRONMENT,
                MeteringEventsProto.ServiceFeature.Value.OBJECT_STORAGE);
        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(STORAGE_CONSUMPTION_COLLECTION_FINISH_EVENT.selector(), result.selector());
    }

    private StorageConsumptionCollectionHandlerEvent createInputEvent(String platform, StorageConsumptionResult storageConsumptionResult) {
        Consumption consumption = new Consumption();
        consumption.setEnvironmentCrn(ENV_CRN);
        consumption.setStorageLocation(VALID_STORAGE_LOCATION);
        consumption.setConsumptionType(ConsumptionType.STORAGE);
        ConsumptionContext context = new ConsumptionContext(null, consumption);
        CloudPlatformConnectors cloudPlatformConnectors = mock(CloudPlatformConnectors.class);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(cloudConnector);
        when(cloudConnector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));
        ReflectionTestUtils.setField(underTest, "cloudPlatformConnectors", cloudPlatformConnectors);

        return new StorageConsumptionCollectionHandlerEvent(
                STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector(),
                ID, CRN, context, storageConsumptionResult == null ? Set.of() : Set.of(storageConsumptionResult));
    }
}

