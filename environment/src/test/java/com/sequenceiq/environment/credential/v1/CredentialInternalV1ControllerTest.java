package com.sequenceiq.environment.credential.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.environment.credential.service.CredentialService;

@ExtendWith(MockitoExtension.class)
class CredentialInternalV1ControllerTest {

    private static final String PLATFORM = "PLATFORM";

    @Mock
    private CredentialService credentialService;

    @InjectMocks
    private CredentialInternalV1Controller underTest;

    @Test
    void testGetPrerequisitesForCloudPlatform() {
        CredentialPrerequisitesResponse credentialPrerequisitesResponse = mock(CredentialPrerequisitesResponse.class);
        when(credentialService.getInternalPrerequisitesForCloudPlatform(PLATFORM, false)).thenReturn(credentialPrerequisitesResponse);

        CredentialPrerequisitesResponse response = underTest.getPrerequisitesForCloudPlatform(PLATFORM, false);

        assertThat(response).isEqualTo(credentialPrerequisitesResponse);

        verify(credentialService).getInternalPrerequisitesForCloudPlatform(PLATFORM, false);
        verifyNoMoreInteractions(credentialService);
    }

}