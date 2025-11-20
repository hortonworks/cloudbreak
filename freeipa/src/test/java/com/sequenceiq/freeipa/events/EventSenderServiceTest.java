package com.sequenceiq.freeipa.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.converter.stack.StackToStackEventConverter;
import com.sequenceiq.freeipa.dto.StackEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.notification.WebSocketNotificationService;

@ExtendWith(MockitoExtension.class)
class EventSenderServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenant:user:user-id";

    private static final String ACCOUNT_ID = "account-id";

    private static final String STACK_NAME = "test-stack";

    private static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:tenant:freeipa:stack-id";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenant:environment:env-id";

    private static final Long STACK_ID = 123L;

    private static final String NODE_ID = "node-1";

    private static final String SERVICE_VERSION = "2.75.0";

    private static final String MESSAGE = "Test message";

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private StackToStackEventConverter stackToStackEventConverter;

    @Mock
    private CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private EventSenderService underTest;

    private Stack stack;

    private StackEvent stackEvent;

    @BeforeEach
    void setUp() throws Exception {
        stack = createStack();
        stackEvent = createStackEvent();

        when(nodeConfig.getId()).thenReturn(NODE_ID);

        // Manually set the serviceVersion field since @Value doesn't work with @InjectMocks
        java.lang.reflect.Field serviceVersionField = EventSenderService.class.getDeclaredField("serviceVersion");
        serviceVersionField.setAccessible(true);
        serviceVersionField.set(underTest, SERVICE_VERSION);
    }

    @Test
    void sendEventAndNotificationWithoutMessageArgs() {
        when(stackToStackEventConverter.convert(stack)).thenReturn(stackEvent);
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        underTest.sendEventAndNotification(stack, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED);

        verify(stackToStackEventConverter).convert(stack);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
        verify(webSocketNotificationService).send(eq(ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED), anyCollection(), eq(stackEvent), eq(USER_CRN),
                isNull());
        verify(cloudbreakMessagesService).getMessage(any(), anyCollection());
    }

    @Test
    void sendEventAndNotificationWithMessageArgs() {
        when(stackToStackEventConverter.convert(stack)).thenReturn(stackEvent);
        List<String> messageArgs = List.of("arg1", "arg2");
        when(cloudbreakMessagesService.getMessage(any(), eq(messageArgs))).thenReturn(MESSAGE);

        underTest.sendEventAndNotification(stack, USER_CRN, ResourceEvent.ENVIRONMENT_START_FREEIPA_STARTED, messageArgs);

        verify(stackToStackEventConverter).convert(stack);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
        verify(webSocketNotificationService).send(eq(ResourceEvent.ENVIRONMENT_START_FREEIPA_STARTED), eq(messageArgs), eq(stackEvent), eq(USER_CRN), isNull());
        verify(cloudbreakMessagesService).getMessage(any(), eq(messageArgs));
    }

    @Test
    void sendEventAndNotificationWithPayloadWithoutMessageArgs() {
        String customPayload = "custom-payload";
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        underTest.sendEventAndNotificationWithPayload(stack, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED, customPayload);

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
        verify(webSocketNotificationService).send(eq(ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED), anyCollection(), eq(customPayload), eq(USER_CRN),
                isNull());
        verify(cloudbreakMessagesService).getMessage(any(), anyCollection());
    }

    @Test
    void sendEventAndNotificationWithPayloadWithMessageArgs() {
        String customPayload = "custom-payload";
        List<String> messageArgs = List.of("arg1", "arg2");
        when(cloudbreakMessagesService.getMessage(any(), eq(messageArgs))).thenReturn(MESSAGE);

        underTest.sendEventAndNotificationWithPayload(stack, USER_CRN, ResourceEvent.ENVIRONMENT_START_FREEIPA_STARTED, customPayload, messageArgs);

        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
        verify(webSocketNotificationService).send(eq(ResourceEvent.ENVIRONMENT_START_FREEIPA_STARTED), eq(messageArgs), eq(customPayload), eq(USER_CRN),
                isNull());
        verify(cloudbreakMessagesService).getMessage(any(), eq(messageArgs));
    }

    @Test
    void sendEventAndNotificationWithoutStack() {
        BaseNamedFlowEvent flowEvent = new BaseNamedFlowEvent("selector", STACK_ID, STACK_NAME, RESOURCE_CRN);
        when(cloudbreakMessagesService.getMessage(any())).thenReturn(MESSAGE);

        underTest.sendEventAndNotificationWithoutStack(flowEvent, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_FAILED, USER_CRN);

        ArgumentCaptor<CDPStructuredNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CDPStructuredNotificationEvent.class);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(eventCaptor.capture());
        verify(webSocketNotificationService).send(eq(ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_FAILED), eq(flowEvent), eq(USER_CRN));
        verify(cloudbreakMessagesService).getMessage(any());

        CDPStructuredNotificationEvent capturedEvent = eventCaptor.getValue();
        CDPOperationDetails operationDetails = capturedEvent.getOperation();
        assert operationDetails.getResourceId().equals(STACK_ID);
        assert operationDetails.getResourceName().equals(STACK_NAME);
        assert operationDetails.getResourceCrn().equals(RESOURCE_CRN);
        assert operationDetails.getAccountId() == null;
    }

    @Test
    void getStructuredEventShouldCreateProperCDPEvent() {
        when(stackToStackEventConverter.convert(stack)).thenReturn(stackEvent);
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
            underTest.sendEventAndNotification(stack, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED)
        );

        ArgumentCaptor<CDPStructuredNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CDPStructuredNotificationEvent.class);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(eventCaptor.capture());

        CDPStructuredNotificationEvent capturedEvent = eventCaptor.getValue();
        CDPOperationDetails operationDetails = capturedEvent.getOperation();

        assert operationDetails.getResourceType().equals("freeipa");
        assert operationDetails.getResourceId().equals(STACK_ID);
        assert operationDetails.getResourceName().equals(STACK_NAME);
        assert operationDetails.getResourceCrn().equals(RESOURCE_CRN);
        assert operationDetails.getAccountId().equals(ACCOUNT_ID);
        assert operationDetails.getCloudbreakId().equals(NODE_ID);
        assert operationDetails.getCloudbreakVersion().equals(SERVICE_VERSION);
        assert operationDetails.getUserCrn().equals(USER_CRN);
        assert operationDetails.getEnvironmentCrn().equals(RESOURCE_CRN);

        CDPStructuredNotificationDetails notificationDetails = capturedEvent.getNotificationDetails();
        assert notificationDetails.getResourceCrn().equals(RESOURCE_CRN);
        assert notificationDetails.getResourceType().equals("freeipa");
        assert notificationDetails.getPayload() != null;
    }

    @Test
    void getNotificationDetailsShouldSerializePayloadSuccessfully() {
        StackEvent payload = createStackEvent();
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        underTest.sendEventAndNotificationWithPayload(stack, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED, payload);

        ArgumentCaptor<CDPStructuredNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CDPStructuredNotificationEvent.class);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(eventCaptor.capture());

        CDPStructuredNotificationEvent capturedEvent = eventCaptor.getValue();
        CDPStructuredNotificationDetails notificationDetails = capturedEvent.getNotificationDetails();

        assert notificationDetails.getPayload().contains("\"id\":123");
        assert notificationDetails.getPayload().contains("\"name\":\"test-stack\"");
        assert notificationDetails.getPayload().contains("\"resourceCrn\"");
    }

    @Test
    void getNotificationDetailsShouldHandleComplexPayload() {
        Object complexPayload = new Object() {
            private final String field1 = "value1";
            private final int field2 = 42;

            @SuppressWarnings("unused")
            public String getField1() {
                return field1;
            }

            @SuppressWarnings("unused")
            public int getField2() {
                return field2;
            }
        };
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        underTest.sendEventAndNotificationWithPayload(stack, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED, complexPayload);

        ArgumentCaptor<CDPStructuredNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CDPStructuredNotificationEvent.class);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(eventCaptor.capture());

        CDPStructuredNotificationEvent capturedEvent = eventCaptor.getValue();
        CDPStructuredNotificationDetails notificationDetails = capturedEvent.getNotificationDetails();

        assert notificationDetails.getPayload() != null;
        assert !notificationDetails.getPayload().isEmpty();
    }

    @Test
    void sendEventAndNotificationShouldCallWebSocketServiceOnce() {
        when(stackToStackEventConverter.convert(stack)).thenReturn(stackEvent);
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        underTest.sendEventAndNotification(stack, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED);

        verify(webSocketNotificationService, times(1)).send(any(ResourceEvent.class), anyCollection(), any(), eq(USER_CRN), isNull());
    }

    @Test
    void sendEventAndNotificationShouldCallStructuredEventClientOnce() {
        when(stackToStackEventConverter.convert(stack)).thenReturn(stackEvent);
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        underTest.sendEventAndNotification(stack, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED);

        verify(cdpDefaultStructuredEventClient, times(1)).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
    }

    @Test
    void createStructureEventForMissingStackShouldUseFlowEventDetails() {
        BaseNamedFlowEvent flowEvent = new BaseNamedFlowEvent("selector", STACK_ID, STACK_NAME, RESOURCE_CRN);
        when(cloudbreakMessagesService.getMessage(any())).thenReturn(MESSAGE);

        underTest.sendEventAndNotificationWithoutStack(flowEvent, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_FAILED, USER_CRN);

        ArgumentCaptor<CDPStructuredNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CDPStructuredNotificationEvent.class);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(eventCaptor.capture());

        CDPStructuredNotificationEvent capturedEvent = eventCaptor.getValue();
        CDPOperationDetails operationDetails = capturedEvent.getOperation();

        assert operationDetails.getResourceType().equals("basenamedflowevent");
        assert operationDetails.getUserCrn().equals(USER_CRN);
        assert operationDetails.getAccountId() == null;
    }

    @Test
    void sendEventWithStackStatusNull() {
        Stack stackWithoutStatus = createStack();
        stackWithoutStatus.setStackStatus(null);
        StackEvent eventWithoutStatus = createStackEvent();
        eventWithoutStatus.setStatus(null);

        when(stackToStackEventConverter.convert(stackWithoutStatus)).thenReturn(eventWithoutStatus);
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn(MESSAGE);

        underTest.sendEventAndNotification(stackWithoutStatus, USER_CRN, ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED);

        verify(stackToStackEventConverter).convert(stackWithoutStatus);
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(any(CDPStructuredNotificationEvent.class));
        verify(webSocketNotificationService).send(eq(ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED), anyCollection(), eq(eventWithoutStatus),
                eq(USER_CRN), isNull());
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform("AWS");
        stack.setRegion("us-west-1");
        stack.setAvailabilityZone("us-west-1a");

        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);

        return stack;
    }

    private StackEvent createStackEvent() {
        StackEvent stackEvent = new StackEvent();
        stackEvent.setId(STACK_ID);
        stackEvent.setName(STACK_NAME);
        stackEvent.setResourceCrn(RESOURCE_CRN);
        stackEvent.setEnvironmentCrn(ENVIRONMENT_CRN);
        stackEvent.setAccountId(ACCOUNT_ID);
        stackEvent.setCloudPlatform("AWS");
        stackEvent.setRegion("us-west-1");
        stackEvent.setAvailabilityZone("us-west-1a");
        stackEvent.setStatus(Status.AVAILABLE);
        return stackEvent;
    }
}