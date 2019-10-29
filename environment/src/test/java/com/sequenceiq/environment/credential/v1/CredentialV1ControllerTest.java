package com.sequenceiq.environment.credential.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.environment.credential.service.CredentialDeleteService;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;

@ExtendWith(MockitoExtension.class)
class CredentialV1ControllerTest {

    private static final String USER_CRN = "USER_CRN";

    private static final String PLATFORM = "PLATFORM";

    private static final String DEPLOYMENT_ADDRESS = "DEPLOYMENT_ADDRESS";

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCredentialV1ResponseConverter credentialConverter;

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    private CredentialDeleteService credentialDeleteService;

    @InjectMocks
    private CredentialV1Controller underTest;

    @Test
    void testGetPrerequisitesForCloudPlatform() {
        when(threadBasedUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        CredentialPrerequisitesResponse credentialPrerequisitesResponse = mock(CredentialPrerequisitesResponse.class);
        when(credentialService.getPrerequisites(PLATFORM, DEPLOYMENT_ADDRESS, USER_CRN)).thenReturn(credentialPrerequisitesResponse);

        CredentialPrerequisitesResponse response = underTest.getPrerequisitesForCloudPlatform(PLATFORM, DEPLOYMENT_ADDRESS);
        assertThat(response).isEqualTo(credentialPrerequisitesResponse);
    }

}