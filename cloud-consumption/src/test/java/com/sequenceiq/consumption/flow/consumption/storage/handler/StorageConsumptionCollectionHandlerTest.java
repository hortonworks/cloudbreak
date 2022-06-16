package com.sequenceiq.consumption.flow.consumption.storage.handler;

import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionHandlerSelectors.STORAGE_CONSUMPTION_COLLECTION_HANDLER;
import static com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors.SEND_CONSUMPTION_EVENT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.validation.ValidationException;
import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageSizeResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private ObjectStorageConnector objectStorageConnector;

    @InjectMocks
    private StorageConsumptionCollectionHandler underTest;

    @Captor
    private ArgumentCaptor<CloudPlatformVariant> variantCaptor;

    @Captor
    private ArgumentCaptor<ObjectStorageSizeRequest> requestCaptor;

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
        mockEnvironmentService(CloudPlatform.AWS.name());
        mockCredentialServices();
        mockCloudConnector();

        ObjectStorageSizeResponse objectStorageSizeResponse = ObjectStorageSizeResponse.builder().withStorageInBytes(STORAGE_SIZE).build();
        when(objectStorageConnector.getObjectStorageSize(any(ObjectStorageSizeRequest.class))).thenReturn(objectStorageSizeResponse);

        StorageConsumptionCollectionHandlerEvent event = createInputEvent();
        SendStorageConsumptionEvent result = (SendStorageConsumptionEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(STORAGE_SIZE, result.getStorageConsumptionResult().getStorageInBytes(), DOUBLE_ASSERT_EPSILON);
        assertEquals(SEND_CONSUMPTION_EVENT_EVENT.selector(), result.selector());

        verify(environmentService).getByCrn(ENV_CRN);

        verify(credentialService).getCredentialByEnvCrn(ENV_CRN);
        verify(credentialConverter).convert(credential);

        verify(cloudPlatformConnectors).get(variantCaptor.capture());
        assertEquals(CloudPlatform.AWS.name(), variantCaptor.getValue().getVariant().value());
        assertEquals(CloudPlatform.AWS.name(), variantCaptor.getValue().getPlatform().value());

        verify(cloudConnector).objectStorage();

        verify(objectStorageConnector).getObjectStorageSize(requestCaptor.capture());
        assertEquals(VALID_BUCKET_NAME, requestCaptor.getValue().getObjectStoragePath());
        assertEquals(cloudCredential, requestCaptor.getValue().getCredential());
        assertEquals(REGION, requestCaptor.getValue().getRegion().getRegionName());
    }

    @Test
    public void testExecuteOperationCloudPlatformNotAws() {
        mockEnvironmentService(CloudPlatform.AZURE.name());

        StorageConsumptionCollectionHandlerEvent event = createInputEvent();
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(UnsupportedOperationException.class, result.getException().getClass());
        assertEquals(String.format("Storage consumption collection is not supported on cloud platform %s", CloudPlatform.AZURE.name()),
                result.getException().getMessage());

        verify(environmentService).getByCrn(ENV_CRN);
    }

    @Test
    public void testExecuteOperationObjectStorageConnectorThrowsException() {
        mockEnvironmentService(CloudPlatform.AWS.name());
        mockCredentialServices();
        mockCloudConnector();

        CloudConnectorException ex = new CloudConnectorException("error");
        when(objectStorageConnector.getObjectStorageSize(any(ObjectStorageSizeRequest.class))).thenThrow(ex);

        StorageConsumptionCollectionHandlerEvent event = createInputEvent();
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(ex, result.getException());

        verify(environmentService).getByCrn(ENV_CRN);

        verify(credentialService).getCredentialByEnvCrn(ENV_CRN);
        verify(credentialConverter).convert(credential);

        verify(cloudPlatformConnectors).get(variantCaptor.capture());
        assertEquals(CloudPlatform.AWS.name(), variantCaptor.getValue().getVariant().value());
        assertEquals(CloudPlatform.AWS.name(), variantCaptor.getValue().getPlatform().value());

        verify(cloudConnector).objectStorage();

        verify(objectStorageConnector).getObjectStorageSize(requestCaptor.capture());
        assertEquals(VALID_BUCKET_NAME, requestCaptor.getValue().getObjectStoragePath());
        assertEquals(cloudCredential, requestCaptor.getValue().getCredential());
        assertEquals(REGION, requestCaptor.getValue().getRegion().getRegionName());
    }

    @Test
    public void testExecuteOperationObjectStorageConnectorNotFound() {
        mockEnvironmentService(CloudPlatform.AWS.name());
        mockCredentialServices();

        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(null);

        StorageConsumptionCollectionHandlerEvent event = createInputEvent();
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(NotFoundException.class, result.getException().getClass());
        assertEquals(String.format("No object storage connector for cloud platform: %s", CloudPlatform.AWS.name()), result.getException().getMessage());

        verify(environmentService).getByCrn(ENV_CRN);

        verify(credentialService).getCredentialByEnvCrn(ENV_CRN);
        verify(credentialConverter).convert(credential);

        verify(cloudPlatformConnectors).get(variantCaptor.capture());
        assertEquals(CloudPlatform.AWS.name(), variantCaptor.getValue().getVariant().value());
        assertEquals(CloudPlatform.AWS.name(), variantCaptor.getValue().getPlatform().value());
    }

    @Test
    public void testExecuteOperationCredentialServiceThrowsException() {
        mockEnvironmentService(CloudPlatform.AWS.name());

        CloudbreakServiceException ex = new CloudbreakServiceException("error");
        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenThrow(ex);

        StorageConsumptionCollectionHandlerEvent event = createInputEvent();
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(ex, result.getException());

        verify(environmentService).getByCrn(ENV_CRN);

        verify(credentialService).getCredentialByEnvCrn(ENV_CRN);
    }

    @Test
    public void testExecuteOperationEnvironmentServiceThrowsException() {
        InternalServerErrorException ex = new InternalServerErrorException("error");
        when(environmentService.getByCrn(ENV_CRN)).thenThrow(ex);

        StorageConsumptionCollectionHandlerEvent event = createInputEvent();
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(ex, result.getException());

        verify(environmentService).getByCrn(ENV_CRN);
    }

    @Test
    public void testExecuteOperationAwsStorageLocationValidationFails() {
        mockEnvironmentService(CloudPlatform.AWS.name());

        StorageConsumptionCollectionHandlerEvent event = createInputEvent(INVALID_STORAGE_LOCATION);
        StorageConsumptionCollectionFailureEvent result = (StorageConsumptionCollectionFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ID, result.getResourceId());
        assertEquals(ValidationException.class, result.getException().getClass());

        verify(environmentService).getByCrn(ENV_CRN);
    }

    private void mockEnvironmentService(String platform) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(platform)
                .withLocation(LocationResponseBuilder.aLocationResponse().withName(REGION).build())
                .build();
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
    }

    private void mockCredentialServices() {
        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
    }

    private void mockCloudConnector() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.objectStorage()).thenReturn(objectStorageConnector);
    }

    private StorageConsumptionCollectionHandlerEvent createInputEvent() {
        return createInputEvent(VALID_STORAGE_LOCATION);
    }

    private StorageConsumptionCollectionHandlerEvent createInputEvent(String storageLocation) {
        Consumption consumption = new Consumption();
        consumption.setEnvironmentCrn(ENV_CRN);
        consumption.setStorageLocation(storageLocation);
        ConsumptionContext context = new ConsumptionContext(null, consumption);
        return new StorageConsumptionCollectionHandlerEvent(
                STORAGE_CONSUMPTION_COLLECTION_HANDLER.selector(),
                ID, CRN, context, null);
    }
}
