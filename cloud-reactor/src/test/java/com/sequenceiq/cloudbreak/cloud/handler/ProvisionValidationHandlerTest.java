package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudPlatformValidationWarningException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
public class ProvisionValidationHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String WARNING_MESSAGE = "warning";

    @InjectMocks
    private ProvisionValidationHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private Validator validator;

    @Test
    void testAcceptShouldSendEventWhenAllValidationWereSuccessful() {
        ValidationRequest request = createValidationRequest();
        CloudContext cloudContext = request.getCloudContext();
        mockCloudConnector(request, cloudContext);

        underTest.accept(new Event<>(request));

        verifyCloudConnectorCalls(request, cloudContext);
        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("VALIDATIONRESULT"), argumentCaptor.capture());
        Event<ValidationResult> resultEvent = argumentCaptor.getValue();
        assertTrue(resultEvent.getData().getWarningMessages().isEmpty());
    }

    @Test
    void testAcceptShouldSendEventWhenTheValidatorThrowsWarningMessage() {
        ValidationRequest request = createValidationRequest();
        CloudContext cloudContext = request.getCloudContext();
        mockCloudConnector(request, cloudContext);
        doThrow(new CloudPlatformValidationWarningException(WARNING_MESSAGE, new Exception()))
                .when(validator).validate(ac, request.getCloudStack());

        underTest.accept(new Event<>(request));

        verifyCloudConnectorCalls(request, cloudContext);
        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("VALIDATIONRESULT"), argumentCaptor.capture());
        Event<ValidationResult> resultEvent = argumentCaptor.getValue();
        ValidationResult validationResult = resultEvent.getData();
        assertTrue(validationResult.getWarningMessages().contains(WARNING_MESSAGE));
        assertEquals(1, validationResult.getWarningMessages().size());
    }

    @Test
    void testAcceptShouldSendFailureEventWhenTheValidationFailed() {
        ValidationRequest request = createValidationRequest();
        CloudContext cloudContext = request.getCloudContext();
        mockCloudConnector(request, cloudContext);
        CloudConnectorException exception = new CloudConnectorException("Error");
        doThrow(exception).when(validator).validate(ac, request.getCloudStack());

        underTest.accept(new Event<>(request));

        verifyCloudConnectorCalls(request, cloudContext);
        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("VALIDATIONRESULT_ERROR"), argumentCaptor.capture());
        Event<ValidationResult> resultEvent = argumentCaptor.getValue();
        assertEquals(exception, resultEvent.getData().getErrorDetails());
    }

    private void verifyCloudConnectorCalls(ValidationRequest request, CloudContext cloudContext) {
        verify(cloudPlatformConnectors).get(cloudContext.getPlatformVariant());
        verify(cloudConnector).authentication();
        verify(authenticator).authenticate(cloudContext, request.getCloudCredential());
        verify(cloudConnector).validators(ValidatorType.ALL);
        verify(validator).validate(ac, request.getCloudStack());
    }

    private void mockCloudConnector(ValidationRequest request, CloudContext cloudContext) {
        when(cloudPlatformConnectors.get(cloudContext.getPlatformVariant())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, request.getCloudCredential())).thenReturn(ac);
        when(cloudConnector.validators(ValidatorType.ALL)).thenReturn(List.of(validator));
    }

    private ValidationRequest createValidationRequest() {
        CloudContext cloudContext = new CloudContext.Builder().withPlatform("AWS").withVariant("AWS").withId(STACK_ID).build();
        return new ValidationRequest(cloudContext, mock(CloudCredential.class), mock(CloudStack.class));
    }

}
