package com.sequenceiq.environment.credential.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

class ServiceProviderCredentialAdapterTest {

    private static final String ACCOUNT_ID = "anAccountID";

    private static final Long CREDENTIAL_ID = 1L;

    private static final String CREDENTIAL_NAME = "someTestCredential";

    private static final String CLOUD_PLATFORM = "AWS";

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Mock
    private RequestProvider requestProvider;

    @InjectMocks
    private ServiceProviderCredentialAdapter underTest;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential convertedCredential;

    @Mock
    private CredentialVerificationRequest credentialVerificationRequest;

    @Mock
    private CredentialVerificationResult credentialVerificationResult;

    @Mock
    private CloudCredentialStatus cloudCredentialStatus;

    @BeforeEach
    void setUp() throws InterruptedException {
        MockitoAnnotations.initMocks(this);
        when(credential.getId()).thenReturn(CREDENTIAL_ID);
        when(credential.getName()).thenReturn(CREDENTIAL_NAME);
        when(credentialVerificationResult.getStatus()).thenReturn(EventStatus.OK);
        when(credentialVerificationResult.getCloudCredentialStatus()).thenReturn(cloudCredentialStatus);
        when(credentialVerificationRequest.await()).thenReturn(credentialVerificationResult);
        when(credentialPrerequisiteService.decorateCredential(any())).thenAnswer(i -> i.getArgument(0));
        when(credentialConverter.convert(credential)).thenReturn(convertedCredential);
        when(requestProvider.getCredentialVerificationRequest(any(CloudContext.class), eq(convertedCredential), anyBoolean()))
                .thenReturn(credentialVerificationRequest);
    }

    @Test
    void testCredentialVerificationCredentialNotChanged() {
        when(cloudCredentialStatus.getCloudCredential()).thenReturn(convertedCredential);
        when(cloudCredentialStatus.getStatus()).thenReturn(CredentialStatus.VERIFIED);
        CredentialVerification verification = underTest.verify(credential, ACCOUNT_ID);
        assertFalse(verification.isChanged());
    }

    @Test
    void testCredentialVerificationPermissionMissing() {
        when(cloudCredentialStatus.getCloudCredential()).thenReturn(convertedCredential);
        when(cloudCredentialStatus.getStatus()).thenReturn(CredentialStatus.PERMISSIONS_MISSING);
        when(cloudCredentialStatus.getStatusReason()).thenReturn(CredentialStatus.PERMISSIONS_MISSING.name());
        CredentialVerification verification = underTest.verify(credential, ACCOUNT_ID);
        assertTrue(verification.isChanged());
    }
}
