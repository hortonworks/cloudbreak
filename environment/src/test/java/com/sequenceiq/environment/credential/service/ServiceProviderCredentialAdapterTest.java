package com.sequenceiq.environment.credential.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationException;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponse;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
import com.sequenceiq.environment.environment.verification.CDPServicePolicyVerification;
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

    @Mock
    private CDPServicePolicyVerificationRequest cdpServicePolicyVerificationRequest;

    @Mock
    private CDPServicePolicyVerificationResult cdpServicePolicyVerificationResult;

    @Mock
    private CDPServicePolicyVerificationResponses cdpServicePolicyVerificationResponses;

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

    @Test
    void testMergeAttributes() {
        when(cloudCredentialStatus.getCloudCredential()).thenReturn(convertedCredential);

        when(cloudCredentialStatus.getStatus()).thenReturn(CredentialStatus.VERIFIED);
        when(cloudCredentialStatus.isDefaultRegionChanged()).thenReturn(true);
        when(credential.getAttributes()).thenReturn("{\"key1\":\"value1\"}");
        when(convertedCredential.getParameters()).thenReturn(Map.of("key2", "value2"));
        CredentialVerification verification = underTest.verify(credential, ACCOUNT_ID);

        assertTrue(verification.isChanged());
        verify(credential).setAttributes("{\"key1\":\"value1\",\"key2\":\"value2\"}");
    }

    @Test
    void testVerifyByServices() throws InterruptedException {
        when(cloudCredentialStatus.getCloudCredential()).thenReturn(convertedCredential);
        when(requestProvider.getCDPServicePolicyVerificationRequest(any(CloudContext.class), eq(convertedCredential),
                anyList(), anyMap())).thenReturn(cdpServicePolicyVerificationRequest);
        when(cdpServicePolicyVerificationRequest.await()).thenReturn(cdpServicePolicyVerificationResult);
        when(cdpServicePolicyVerificationResult.getStatus()).thenReturn(EventStatus.OK);
        when(cdpServicePolicyVerificationResult.getCdpServicePolicyVerificationResponses()).thenReturn(cdpServicePolicyVerificationResponses);

        CDPServicePolicyVerificationResponse r1 = new CDPServicePolicyVerificationResponse();
        r1.setServiceName("service1");

        CDPServicePolicyVerificationResponse r2 = new CDPServicePolicyVerificationResponse();
        r1.setServiceName("service2");

        Set<CDPServicePolicyVerificationResponse> expected = Set.of(r1, r2);

        when(cdpServicePolicyVerificationResponses.getResults()).thenReturn(Set.of(r1, r2));
        CDPServicePolicyVerification verification = underTest.verifyByServices(credential, ACCOUNT_ID, List.of("service1", "service2"),
                Map.of("key3", "value3"));

        assertEquals(2, verification.getResults().size());
        assertEquals(expected, verification.getResults());
    }

    @Test
    void testFailedVerifyByServices() throws InterruptedException {
        when(cloudCredentialStatus.getCloudCredential()).thenReturn(convertedCredential);
        when(requestProvider.getCDPServicePolicyVerificationRequest(any(CloudContext.class), eq(convertedCredential),
                anyList(), anyMap())).thenReturn(cdpServicePolicyVerificationRequest);
        when(cdpServicePolicyVerificationRequest.await()).thenReturn(cdpServicePolicyVerificationResult);
        when(cdpServicePolicyVerificationResult.getStatus()).thenReturn(EventStatus.FAILED);
        when(cdpServicePolicyVerificationResult.getErrorDetails()).thenReturn(new Exception("details"));

        CDPServicePolicyVerificationException exc = assertThrows(CDPServicePolicyVerificationException.class,
                () -> underTest.verifyByServices(credential, ACCOUNT_ID,
                List.of("service1", "service2"), Map.of("key3", "value3")));

        assertEquals("Failed to verify the policy (try few minutes later if policies and" +
                " roles are newly created or modified): java.lang.Exception: details", exc.getMessage());
    }
}
