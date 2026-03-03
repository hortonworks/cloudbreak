package com.sequenceiq.environment.credential.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.environment.verification.PolicyValidationErrorResponseConverter;

@ExtendWith(MockitoExtension.class)
class CredentialServicePrerequisiteTest {

    private static final String CLOUD_PLATFORM = "AWS";

    private static final boolean GOV_CLOUD = false;

    private static final CredentialType CREDENTIAL_TYPE = CredentialType.ENVIRONMENT;

    @Mock
    private CredentialValidator mockCredentialValidator;

    @Mock
    private ServiceProviderCredentialAdapter mockCredentialAdapter;

    @Mock
    private CredentialPrerequisiteService mockCredentialPrerequisiteService;

    @Mock
    private PolicyValidationErrorResponseConverter mockPolicyValidationErrorResponseConverter;

    @Mock
    private CredentialCreateService credentialCreateService;

    @Mock
    private CredentialRetrievalService credentialRetrievalService;

    @Mock
    private CredentialUpdateService credentialUpdateService;

    private CredentialService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CredentialService(mockCredentialValidator, mockCredentialAdapter, mockCredentialPrerequisiteService,
                mockPolicyValidationErrorResponseConverter, credentialCreateService, credentialRetrievalService, credentialUpdateService);
    }

    @Test
    @DisplayName("Test to ensure getPrerequisites method calls the necessary and designated CredentialValidator and CredentialPrerequisiteService methods")
    void testGetPrerequisitesForCloudPlatform() {
        String deploymentAddress = "deploymentAddress";
        underTest.getPrerequisites(CLOUD_PLATFORM, GOV_CLOUD, deploymentAddress, CREDENTIAL_TYPE);

        verify(mockCredentialValidator).validateCredentialCloudPlatform(CLOUD_PLATFORM);
        verifyNoMoreInteractions(mockCredentialValidator);
        verify(mockCredentialPrerequisiteService).getPrerequisites(CLOUD_PLATFORM, GOV_CLOUD, deploymentAddress, CREDENTIAL_TYPE);
        verifyNoMoreInteractions(mockCredentialPrerequisiteService);
    }

    @Test
    @DisplayName("Test to ensure getInternalPrerequisitesForCloudPlatform method calls the necessary and designated CredentialValidator and " +
            "CredentialPrerequisiteService methods")
    void testGetInternalPrerequisitesForCloudPlatform() {
        boolean internal = true;
        underTest.getInternalPrerequisitesForCloudPlatform(CLOUD_PLATFORM, GOV_CLOUD);

        verify(mockCredentialValidator).validateCredentialCloudPlatform(CLOUD_PLATFORM);
        verifyNoMoreInteractions(mockCredentialValidator);
        verify(mockCredentialPrerequisiteService).getPrerequisites(CLOUD_PLATFORM, GOV_CLOUD, null, CREDENTIAL_TYPE, internal);
        verifyNoMoreInteractions(mockCredentialPrerequisiteService);
    }

}