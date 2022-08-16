package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.consumption.AwsS3ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.Credential;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;
import com.sequenceiq.consumption.flow.consumption.storage.event.SendStorageConsumptionEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionFailureEvent;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerEvent;
import com.sequenceiq.consumption.service.CredentialService;
import com.sequenceiq.consumption.service.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse.LocationResponseBuilder;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class StorageConsumptionCollectionHandlerTest {

    private static final String ENV_CRN = "env-crn";

    private static final String REGION = "region";

    private static final String CRN = "consumption-crn";

    private static final Long ID = 42L;

    private static final String VALID_STORAGE_LOCATION = "s3a://bucket-name/folder/file";

    private static final String INVALID_STORAGE_LOCATION = "s3://bucket-name/folder/file";

    private static final String VALID_BUCKET_NAME = "bucket-name";

    private static final double STORAGE_SIZE = 42.0;

    private static final double DOUBLE_ASSERT_EPSILON = 0.001;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AwsS3ConsumptionCalculator awsS3ConsumptionCalculator;

    @InjectMocks
    private StorageConsumptionCollectionHandler underTest;

    @Captor
    private ArgumentCaptor<CloudPlatformVariant> variantCaptor;

    @Captor
    private ArgumentCaptor<StorageSizeRequest> requestCaptor;

    @Test
    public void testSelector() {
        assertEquals(STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector(), underTest.selector());
    }

    @Test
    public void testOperationName() {
        assertEquals("Collect storage consumption data", underTest.getOperationName());
    }

    @Test
    public void testExecuteOperationWorksCorrectly() {
        mockEnvironmentService(CloudPlatform.AWS);
        mockCredentialServices();

        StorageSizeResponse objectStorageSizeResponse = StorageSizeResponse.builder().withStorageInBytes(STORAGE_SIZE).build();
        when(awsS3ConsumptionCalculator.calculate(any(StorageSizeRequest.class))).thenReturn(objectStorageSizeResponse);
        when(awsS3ConsumptionCalculator.getObjectId(VALID_STORAGE_LOCATION)).thenReturn(VALID_BUCKET_NAME);
        StorageConsumptionCollectionHandlerEvent event = createInputEvent(ConsumptionType.STORAGE);
        SendStorageConsumptionEvent result = (SendStorageConsumptionEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(STORAGE_SIZE, result.getStorageConsumptionResult().getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
        assertEquals(SEND_CONSUMPTION_EVENT_EVENT.selector(), result.selector());

        verify(environmentService).getByCrn(ENV_CRN);
        verify(credentialService).getCredentialByEnvCrn(ENV_CRN);
        verify(credentialConverter).convert(credential);
        verify(awsS3ConsumptionCalculator).calculate(requestCaptor.capture());
        assertEquals(VALID_BUCKET_NAME, requestCaptor.getValue().getObjectStoragePath());
        assertEquals(cloudCredential, requestCaptor.getValue().getCredential());
        assertEquals(REGION, requestCaptor.getValue().getRegion().getRegionName());
    }

    @Test
    public void testExecuteOperationCloudPlatformNotAws() {
        mockEnvironmentService(CloudPlatform.MOCK);

        StorageConsumptionCollectionHandlerEvent event = createInputEvent(ConsumptionType.UNKNOWN);
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(UnsupportedOperationException.class, result.getException().getClass());
        assertEquals(String.format("Storage consumption collection is not supported on cloud platform %s", CloudPlatform.MOCK.name()),
                result.getException().getMessage());

        verify(environmentService).getByCrn(ENV_CRN);
    }

    @Test
    public void testExecuteOperationObjectStorageConnectorThrowsException() {
        mockEnvironmentService(CloudPlatform.AWS);
        mockCredentialServices();

        CloudConnectorException ex = new CloudConnectorException("error");
        when(awsS3ConsumptionCalculator.calculate(any(StorageSizeRequest.class))).thenThrow(ex);
        when(awsS3ConsumptionCalculator.getObjectId(VALID_STORAGE_LOCATION)).thenReturn(VALID_BUCKET_NAME);
        StorageConsumptionCollectionHandlerEvent event = createInputEvent(ConsumptionType.STORAGE);
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(ex, result.getException());

        verify(environmentService).getByCrn(ENV_CRN);
        verify(credentialService).getCredentialByEnvCrn(ENV_CRN);
        verify(credentialConverter).convert(credential);

        verify(awsS3ConsumptionCalculator).calculate(requestCaptor.capture());
        assertEquals(VALID_BUCKET_NAME, requestCaptor.getValue().getObjectStoragePath());
        assertEquals(cloudCredential, requestCaptor.getValue().getCredential());
        assertEquals(REGION, requestCaptor.getValue().getRegion().getRegionName());
    }

    @Test
    public void testExecuteOperationCredentialServiceThrowsException() {
        Consumption consumption = new Consumption();
        consumption.setEnvironmentCrn(ENV_CRN);
        ConsumptionContext context = new ConsumptionContext(null, consumption);
        CloudbreakServiceException ex = new CloudbreakServiceException("error");
        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenThrow(ex);

        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent)
                underTest.doAccept(new HandlerEvent<>(new Event<>(new StorageConsumptionCollectionHandlerEvent(
                STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector(),
                ID, CRN, context, null))));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(ex, result.getException());

        verify(credentialService).getCredentialByEnvCrn(ENV_CRN);
    }

    @Test
    public void testExecuteOperationEnvironmentServiceThrowsException() {
        InternalServerErrorException ex = new InternalServerErrorException("error");
        when(environmentService.getByCrn(ENV_CRN)).thenThrow(ex);

        StorageConsumptionCollectionHandlerEvent event = createInputEvent(ConsumptionType.STORAGE);
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(ex, result.getException());

        verify(environmentService).getByCrn(ENV_CRN);
    }

    private void mockEnvironmentService(CloudPlatform cloudPlatform) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(cloudPlatform.name())
                .withLocation(LocationResponseBuilder.aLocationResponse().withName(REGION).build())
                .build();
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);

        if (cloudPlatform.equals(CloudPlatform.AWS)) {
            when(awsS3ConsumptionCalculator.calculate(any())).thenReturn(StorageSizeResponse.builder().withStorageInBytes(250).build());
            CloudPlatformConnectors cloudPlatformConnectors = mock(CloudPlatformConnectors.class);
            CloudConnector connector = mock(CloudConnector.class);
            when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
            when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));
            ReflectionTestUtils.setField(underTest, "cloudPlatformConnectors", cloudPlatformConnectors);
        } else {
            CloudPlatformConnectors cloudPlatformConnectors = mock(CloudPlatformConnectors.class);
            CloudConnector connector = mock(CloudConnector.class);
            when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
            when(connector.consumptionCalculator(any())).thenReturn(Optional.empty());
            ReflectionTestUtils.setField(underTest, "cloudPlatformConnectors", cloudPlatformConnectors);
        }
    }

    private void mockCredentialServices() {
        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
    }

    private StorageConsumptionCollectionHandlerEvent createInputEvent(ConsumptionType consumptionType) {
        Consumption consumption = new Consumption();
        consumption.setEnvironmentCrn(ENV_CRN);
        consumption.setStorageLocation(VALID_STORAGE_LOCATION);
        consumption.setConsumptionType(consumptionType);
        ConsumptionContext context = new ConsumptionContext(null, consumption);

        return new StorageConsumptionCollectionHandlerEvent(
                STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector(),
                ID, CRN, context, null);
    }
}
