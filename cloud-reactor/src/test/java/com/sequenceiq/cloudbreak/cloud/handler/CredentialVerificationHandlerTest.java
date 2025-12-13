package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@ExtendWith(MockitoExtension.class)
public class CredentialVerificationHandlerTest {

    @InjectMocks
    private CredentialVerificationHandler underTest;

    @ParameterizedTest
    @EnumSource(value = CredentialStatus.class, names = "FAILED", mode = EnumSource.Mode.EXCLUDE)
    public void testCreateCredentialVerificationResultWhenMissingPermissionAndExceptionNotNull(CredentialStatus status) {
        long resourceId = 123L;

        CloudCredential cloudResource = mock(CloudCredential.class);
        CloudCredentialStatus credentialStatus = new CloudCredentialStatus(cloudResource, status, new Exception(), "reason");
        CredentialVerificationRequest request = mock(CredentialVerificationRequest.class);

        when(request.getResourceId()).thenReturn(resourceId);

        CredentialVerificationResult actual = underTest.createCredentialVerificationResult(request, credentialStatus);

        assertEquals(credentialStatus, actual.getCloudCredentialStatus());
        assertNull(actual.getErrorDetails());
    }

    @Test
    public void testCreateCredentialVerificationResultWhenFailedAndExceptionNotNull() {
        long resourceId = 123L;

        CloudCredential cloudResource = mock(CloudCredential.class);
        CloudCredentialStatus credentialStatus = new CloudCredentialStatus(cloudResource, CredentialStatus.FAILED, new Exception(), "reason");
        CredentialVerificationRequest request = mock(CredentialVerificationRequest.class);

        when(request.getResourceId()).thenReturn(resourceId);

        CredentialVerificationResult actual = underTest.createCredentialVerificationResult(request, credentialStatus);

        assertEquals(credentialStatus, actual.getCloudCredentialStatus());
        assertNotNull(actual.getErrorDetails());
    }
}
