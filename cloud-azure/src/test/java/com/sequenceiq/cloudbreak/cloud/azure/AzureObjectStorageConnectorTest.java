package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.azure.core.http.HttpResponse;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureIDBrokerObjectStorageValidator;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;

@ExtendWith(MockitoExtension.class)
public class AzureObjectStorageConnectorTest {

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AzureIDBrokerObjectStorageValidator azureIDBrokerObjectStorageValidator;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @InjectMocks
    private AzureObjectStorageConnector underTest;

    @BeforeEach
    public void setup() {
        when(entitlementService.azureCloudStorageValidationEnabled(anyString())).thenReturn(Boolean.TRUE);
        lenient().when(azureUtils.convertToCloudConnectorException(any(), anyString())).thenReturn(new CloudConnectorException("cce"));
        lenient().doAnswer(invocation -> ((Supplier) invocation.getArguments()[0]).get()).when(azureExceptionHandler).handleException(any(Supplier.class));
    }

    @Test
    public void testGeneralError() {
        mockIDBrokerStorageValidationError(500, null);
        assertThrows(CloudConnectorException.class, () -> underTest.validateObjectStorage(getRequest()));
        verify(azureUtils).convertToCloudConnectorException(any(), anyString());
    }

    @Test
    public void testGeneralErrorWithCloudError() {
        ApiError apiError = AzureTestUtils.apiError("RandomError", null);
        mockIDBrokerStorageValidationError(500, apiError);
        assertThrows(CloudConnectorException.class, () -> underTest.validateObjectStorage(getRequest()));
        verify(azureUtils).convertToCloudConnectorException(any(), anyString());
    }

    @Test
    public void testAuthorizationFailure() {
        ApiError apiError = AzureTestUtils.apiError("AuthorizationFailed", null);
        mockIDBrokerStorageValidationError(403, apiError);
        when(azureExceptionHandler.isForbidden(any())).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> underTest.validateObjectStorage(getRequest()));
        verify(azureUtils, times(0)).convertToCloudConnectorException(any(), anyString());
    }

    private ObjectStorageValidateRequest getRequest() {
        ObjectStorageValidateRequest objectStorageValidateRequest = new ObjectStorageValidateRequest();
        CloudCredential credential = new CloudCredential();
        credential.setId("crn:cdp:environments:us-west-1:someone:credential:12345");
        objectStorageValidateRequest.setCredential(credential);
        return objectStorageValidateRequest;
    }

    private void mockIDBrokerStorageValidationError(int code, ApiError cloudError) {
        HttpResponse response = mock(HttpResponse.class);
        lenient().when(response.getStatusCode()).thenReturn(code);
        when(azureIDBrokerObjectStorageValidator.validateObjectStorage(any(), any(), any(), any(), any(), any())).thenThrow(
                new ApiErrorException("error", response, cloudError));
    }
}
