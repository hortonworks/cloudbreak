package com.sequenceiq.environment.credential.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowResponse;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

class ServiceProviderCredentialAdapterTest {

    private static final String ACCOUNT_ID = "anAccountID";

    private static final String USER_ID = "1";

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
    private InitCodeGrantFlowRequest initCodeGrantFlowRequest;

    @Mock
    private InitCodeGrantFlowResponse initCodeGrantFlowResponse;

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
        when(initCodeGrantFlowResponse.getStatus()).thenReturn(EventStatus.OK);
        when(initCodeGrantFlowRequest.await()).thenReturn(initCodeGrantFlowResponse);
        when(credentialVerificationResult.getStatus()).thenReturn(EventStatus.OK);
        when(credentialVerificationResult.getCloudCredentialStatus()).thenReturn(cloudCredentialStatus);
        when(credentialVerificationRequest.await()).thenReturn(credentialVerificationResult);
        when(credentialPrerequisiteService.decorateCredential(any())).thenAnswer(i -> i.getArgument(0));
        when(credentialConverter.convert(credential)).thenReturn(convertedCredential);
        when(requestProvider.getInitCodeGrantFlowRequest(any(CloudContext.class), eq(convertedCredential))).thenReturn(initCodeGrantFlowRequest);
        when(requestProvider.getCredentialVerificationRequest(any(CloudContext.class), eq(convertedCredential))).thenReturn(credentialVerificationRequest);
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
    void testInitCodeGrantFlowWhenInitCodeGrantFlowRequestFailsOnAwaitWithInterruptedExceptionThenOperationExceptionComes() throws InterruptedException {
        doThrow(new InterruptedException("Error while executing initialization of authorization code grant based credential creation:"))
                .when(initCodeGrantFlowRequest).await();

        OperationException operationException = assertThrows(OperationException.class, () -> underTest.initCodeGrantFlow(credential, ACCOUNT_ID, USER_ID));
        assertThat(operationException.getMessage())
                .contains("Error while executing initialization of authorization code grant based credential creation:");
    }

    @Test
    void testInitCodeGrantFlowWhenInitCodeGrantFlowResponseStatusIsOkThenBadRequestExceptionComes() {
        Exception exceptionFromResponse = new RuntimeException("some reason");
        when(initCodeGrantFlowResponse.getErrorDetails()).thenReturn(exceptionFromResponse);
        when(initCodeGrantFlowResponse.getStatus()).thenReturn(EventStatus.FAILED);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.initCodeGrantFlow(credential, ACCOUNT_ID, USER_ID));
        assertThat(badRequestException.getMessage())
                .contains(String.format("Authorization code grant based credential creation couldn't be initialized: %s", exceptionFromResponse));
    }

    @Test
    void testInitCodeGrantFlowShouldAddAdditionalAttributesToAzureCodeGrantFlowAttributes() {
        String expectedAdditionalAttributeKey = "someCloudCredentialKey";
        String expectedAdditionalAttributeValue = "someCloudCredentialValue";
        String initialAttributeKey = "aKey";
        String initialAttributeValue = "aValue";

        Map<String, Object> initialAttributes = new HashMap<>();
        Map<String, Object> initialAzureAttributes = new HashMap<>();
        Map<String, String> initialAzureCodeGrantFlowParams = new HashMap<>();
        initialAzureCodeGrantFlowParams.put(initialAttributeKey, initialAttributeValue);
        initialAzureAttributes.put("codeGrantFlowBased", initialAzureCodeGrantFlowParams);
        initialAttributes.put("azure", initialAzureAttributes);

        Credential credential = new Credential();
        String initialCredentialAttriute = new Json(initialAttributes).getValue();
        credential.setAttributes(initialCredentialAttriute);
        credential.setId(CREDENTIAL_ID);
        credential.setCloudPlatform(CLOUD_PLATFORM);
        credential.setName(CREDENTIAL_NAME);
        when(credentialConverter.convert(credential)).thenReturn(convertedCredential);
        when(initCodeGrantFlowResponse.getCodeGrantFlowInitParams()).thenReturn(Map.of(expectedAdditionalAttributeKey, expectedAdditionalAttributeValue));

        Credential result = underTest.initCodeGrantFlow(credential, ACCOUNT_ID, USER_ID);

        assertNotEquals(initialCredentialAttriute, result.getAttributes());
        var attributeMap = new Json(result.getAttributes()).getMap();
        Map<String, Object> azureAttributes = (Map<String, Object>) attributeMap.get("azure");
        Map<String, String> codeGrattFlowAttributes = (Map<String, String>) azureAttributes.get("codeGrantFlowBased");
        assertTrue(codeGrattFlowAttributes.containsKey(initialAttributeKey));
        assertTrue(codeGrattFlowAttributes.containsKey(expectedAdditionalAttributeKey));
        assertEquals(expectedAdditionalAttributeValue, codeGrattFlowAttributes.get(expectedAdditionalAttributeKey));
        assertEquals(initialAttributeValue, codeGrattFlowAttributes.get(initialAttributeKey));
        assertEquals(credential, result);
    }
}
