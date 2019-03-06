package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowResponse;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;

import reactor.bus.EventBus;

public class ServiceProviderCredentialAdapterTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String USER_ID = "1";

    private static final Long CREDENTIAL_ID = 1L;

    private static final String CREDENTIAL_NAME = "someTestCredential";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String CREDENTIAL_OWNER = "theOwnerOfTheCredential";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

    @Before
    public void setUp() throws InterruptedException {
        MockitoAnnotations.initMocks(this);
        when(credential.getId()).thenReturn(CREDENTIAL_ID);
        when(credential.getName()).thenReturn(CREDENTIAL_NAME);
        when(credential.cloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(initCodeGrantFlowResponse.getStatus()).thenReturn(EventStatus.OK);
        when(initCodeGrantFlowRequest.await()).thenReturn(initCodeGrantFlowResponse);
        when(credentialConverter.convert(credential)).thenReturn(convertedCredential);
        when(requestProvider.getInitCodeGrantFlowRequest(any(CloudContext.class), eq(convertedCredential))).thenReturn(initCodeGrantFlowRequest);
    }

    @Test
    public void testInitCodeGrantFlowWhenInitCodeGrantFlowRequestFailsOnAwaitWithInterruptedExceptionThenOperationExceptionComes() throws InterruptedException {
        doThrow(new InterruptedException("Error while executing initialization of authorization code grant based credential creation:"))
                .when(initCodeGrantFlowRequest).await();

        thrown.expect(OperationException.class);
        thrown.expectMessage("Error while executing initialization of authorization code grant based credential creation:");

        underTest.initCodeGrantFlow(credential, WORKSPACE_ID, USER_ID);
    }

    @Test
    public void testInitCodeGrantFlowWhenInitCodeGrantFlowResponseStatusIsOkThenBadRequestExceptionComes() {
        Exception exceptionFromResponse = new RuntimeException("some reason");
        when(initCodeGrantFlowResponse.getErrorDetails()).thenReturn(exceptionFromResponse);
        when(initCodeGrantFlowResponse.getStatus()).thenReturn(EventStatus.FAILED);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("Authorization code grant based credential creation couldn't be initialized: %s", exceptionFromResponse));

        underTest.initCodeGrantFlow(credential, WORKSPACE_ID, USER_ID);
    }

    @Test
    public void testInitCodeGrantFlowWhenasd() {
        String expectedAdditionalAttributeKey = "someCloudCredentialKey";
        String expectedAdditionalAttributeValue = "someCloudCredentialValue";
        String initialAttributeKey = "somekey";
        String initialAttributeValue = "somevalue";
        String initialCredentialAttriute = String.format("{\"%s\":\"%s\"}", initialAttributeKey, initialAttributeValue);
        Credential credential = new Credential();
        credential.setAttributes(initialCredentialAttriute);
        credential.setId(CREDENTIAL_ID);
        credential.setCloudPlatform(CLOUD_PLATFORM);
        credential.setName(CREDENTIAL_NAME);
        when(credentialConverter.convert(credential)).thenReturn(convertedCredential);
        when(convertedCredential.getParameters()).thenReturn(Map.of(expectedAdditionalAttributeKey, expectedAdditionalAttributeValue));

        Credential result = underTest.initCodeGrantFlow(credential, WORKSPACE_ID, USER_ID);

        assertNotEquals(initialCredentialAttriute, result.getAttributes());
        var attributeMap = new Json(result.getAttributes()).getMap();
        assertTrue(attributeMap.containsKey(initialAttributeKey));
        assertTrue(attributeMap.containsKey(expectedAdditionalAttributeKey));
        assertEquals(expectedAdditionalAttributeValue, attributeMap.get(expectedAdditionalAttributeKey));
        assertEquals(initialAttributeValue, attributeMap.get(initialAttributeKey));
        assertEquals(credential, result);
    }

}