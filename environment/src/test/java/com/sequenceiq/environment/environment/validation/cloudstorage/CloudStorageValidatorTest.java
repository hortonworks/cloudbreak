package com.sequenceiq.environment.environment.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentCloudStorageValidationRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.service.cloudstorage.CloudStorageValidator;

@ExtendWith(MockitoExtension.class)
public class CloudStorageValidatorTest {

    @Mock
    private CredentialService credentialService;

    @Mock
    private CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private CloudStorageValidator underTest;

    @Test
    public void validateCloudStorageSkipLocationBaseWhenLoggingIsNotConfigured() {
        when(credentialService.getByCrnForAccountId(anyString(), anyString(), any(), anyBoolean())).thenReturn(new Credential());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        EnvironmentCloudStorageValidationRequest request = new EnvironmentCloudStorageValidationRequest();
        request.setCredentialCrn("credential");
        ArgumentCaptor<ObjectStorageValidateRequest> requestCaptor = ArgumentCaptor.forClass(ObjectStorageValidateRequest.class);
        when(cloudProviderServicesV4Endpoint.validateObjectStorage(requestCaptor.capture()))
                .thenReturn(ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.OK).build());

        ObjectStorageValidateResponse response = underTest.validateCloudStorage("1234", request);

        assertEquals(ResponseStatus.OK, response.getStatus());
        assertNull(response.getError());
        assertNull(requestCaptor.getValue().getLogsLocationBase());
    }

    @Test
    public void validateCloudStorageSetLocationBaseWhenLoggingIsConfigured() {
        when(credentialService.getByCrnForAccountId(anyString(), anyString(), any(), anyBoolean())).thenReturn(new Credential());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        EnvironmentCloudStorageValidationRequest request = new EnvironmentCloudStorageValidationRequest();
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setStorageLocation("s3://mybucket/location");
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile("instanceProfile");
        loggingRequest.setS3(s3CloudStorageV1Parameters);
        telemetryRequest.setLogging(loggingRequest);
        request.setTelemetry(telemetryRequest);
        request.setCredentialCrn("credential");
        ArgumentCaptor<ObjectStorageValidateRequest> requestCaptor = ArgumentCaptor.forClass(ObjectStorageValidateRequest.class);
        when(cloudProviderServicesV4Endpoint.validateObjectStorage(requestCaptor.capture()))
                .thenReturn(ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.OK).build());

        ObjectStorageValidateResponse response = underTest.validateCloudStorage("1234", request);

        assertEquals(ResponseStatus.OK, response.getStatus());
        assertNull(response.getError());
        ObjectStorageValidateRequest objectStorageValidateRequest = requestCaptor.getValue();
        assertEquals("s3://mybucket/location", objectStorageValidateRequest.getLogsLocationBase());
        List<StorageIdentityBase> storageIdentities = objectStorageValidateRequest.getCloudStorageRequest().getIdentities();
        assertEquals(1, storageIdentities.size());
        StorageIdentityBase storageIdentity = storageIdentities.get(0);
        assertEquals(CloudIdentityType.LOG, storageIdentity.getType());
        assertEquals("instanceProfile", storageIdentity.getS3().getInstanceProfile());
    }
}
