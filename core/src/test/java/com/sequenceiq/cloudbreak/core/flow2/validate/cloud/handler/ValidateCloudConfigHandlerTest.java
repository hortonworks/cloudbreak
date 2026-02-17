package com.sequenceiq.cloudbreak.core.flow2.validate.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.datalake.DataLakeValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidatorAndUpdater;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.event.ValidateCloudConfigRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.validation.ParcelValidationAndFilteringService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ValidateCloudConfigHandlerTest {

    private static final Long RESOURCE_ID = 1L;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private StackService stackService;

    @Mock
    private ParametersValidator parametersValidator;

    @Mock
    private StackValidator stackValidator;

    @Mock
    private TemplateValidatorAndUpdater templateValidatorAndUpdater;

    @Mock
    private ClusterCreationEnvironmentValidator environmentValidator;

    @Mock
    private DataLakeValidator dataLakeValidator;

    @Mock
    private MultiAzValidator multiAzValidator;

    @Mock
    private ParcelValidationAndFilteringService parcelValidationAndFilteringService;

    @InjectMocks
    private ValidateCloudConfigHandler underTest;

    private Stack stack;

    private DetailedEnvironmentResponse environment;

    private Credential credential;

    private CloudCredential cloudCredential;

    @BeforeEach
    void setUp() {
        stack = mock(Stack.class);
        lenient().when(stack.getId()).thenReturn(RESOURCE_ID);
        lenient().when(stack.getName()).thenReturn("stackName");
        lenient().when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        lenient().when(stack.getType()).thenReturn(StackType.WORKLOAD);

        environment = new DetailedEnvironmentResponse();
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        environment.setCredential(credentialResponse);
        credential = mock(Credential.class);
        cloudCredential = mock(CloudCredential.class);

        lenient().when(stackService.getByIdWithLists(RESOURCE_ID)).thenReturn(stack);
        lenient().when(environmentClientService.getByCrn("envCrn")).thenReturn(environment);
        lenient().when(credentialConverter.convert(any(CredentialResponse.class))).thenReturn(credential);
        lenient().when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
    }

    @Test
    void testDoAcceptSuccess() {
        InstanceGroup ig = mock(InstanceGroup.class);
        when(stack.getInstanceGroups()).thenReturn(Set.of(ig));
        ParametersValidationRequest parametersValidationRequest = mock(ParametersValidationRequest.class);
        when(parametersValidator.validate(any(), any(), any(), any(), anyLong())).thenReturn(parametersValidationRequest);

        ValidateCloudConfigRequest request = new ValidateCloudConfigRequest(RESOURCE_ID);
        HandlerEvent<ValidateCloudConfigRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);

        assertEquals("VALIDATE_CLOUD_CONFIG_FINISHED_EVENT", result.selector());
        assertEquals(RESOURCE_ID, ((StackEvent) result).getResourceId());

        verify(stackValidator).validate(eq(stack), any(ValidationResult.ValidationResultBuilder.class));
        verify(templateValidatorAndUpdater).validate(eq(environment), eq(credential), eq(ig), eq(stack), any(), any());
        verify(multiAzValidator).validateMultiAzForStack(eq(stack), any());
        verify(parametersValidator).validate(any(), any(), eq(cloudCredential), any(), any());
        verify(parametersValidator).waitResult(eq(parametersValidationRequest), any());
        verify(dataLakeValidator).validate(eq(stack), any());
        verify(environmentValidator).validate(eq(stack), eq(environment), eq(true), any());
        verify(parcelValidationAndFilteringService).validate(eq(stack), any());
    }

    @Test
    void testDoAcceptSuccessWhenStackIsLegacy() {
        InstanceGroup ig = mock(InstanceGroup.class);
        when(stack.getInstanceGroups()).thenReturn(Set.of(ig));
        when(stack.getType()).thenReturn(StackType.LEGACY);
        ParametersValidationRequest parametersValidationRequest = mock(ParametersValidationRequest.class);
        when(parametersValidator.validate(any(), any(), any(), any(), anyLong())).thenReturn(parametersValidationRequest);

        ValidateCloudConfigRequest request = new ValidateCloudConfigRequest(RESOURCE_ID);
        HandlerEvent<ValidateCloudConfigRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);

        assertEquals("VALIDATE_CLOUD_CONFIG_FINISHED_EVENT", result.selector());
        assertEquals(RESOURCE_ID, ((StackEvent) result).getResourceId());

        verify(stackValidator).validate(eq(stack), any(ValidationResult.ValidationResultBuilder.class));
        verify(templateValidatorAndUpdater).validate(eq(environment), eq(credential), eq(ig), eq(stack), any(), any());
        verify(multiAzValidator).validateMultiAzForStack(eq(stack), any());
        verify(parametersValidator).validate(any(), any(), eq(cloudCredential), any(), any());
        verify(parametersValidator).waitResult(eq(parametersValidationRequest), any());
        verify(environmentValidator).validate(eq(stack), eq(environment), eq(false), any());
        verify(parcelValidationAndFilteringService).validate(eq(stack), any());
        verifyNoInteractions(dataLakeValidator);
    }

    @Test
    void testDoAcceptStackValidationError() {
        when(stack.getInstanceGroups()).thenReturn(Collections.emptySet());
        Answer<Void> errorAnswer = invocation -> {
            ValidationResult.ValidationResultBuilder builder = invocation.getArgument(1);
            builder.error("error");
            return null;
        };
        org.mockito.Mockito.doAnswer(errorAnswer).when(stackValidator).validate(any(), any());

        ValidateCloudConfigRequest request = new ValidateCloudConfigRequest(RESOURCE_ID);
        HandlerEvent<ValidateCloudConfigRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> underTest.doAccept(handlerEvent));
        assertEquals("error", exception.getMessage());
    }

    @Test
    void testDoAcceptTemplateValidationError() {
        InstanceGroup ig1 = new InstanceGroup();
        ig1.setStack(stack);
        ig1.setGroupName("ig1");
        InstanceGroup ig2 = new InstanceGroup();
        ig2.setGroupName("ig2");
        ig2.setStack(stack);
        when(stack.getInstanceGroups()).thenReturn(Set.of(ig1, ig2));
        Answer<Void> errorAnswer = invocation -> {
            ValidationResult.ValidationResultBuilder builder = invocation.getArgument(5, ValidationResult.ValidationResultBuilder.class);
            InstanceGroup instanceGroup = invocation.getArgument(2, InstanceGroup.class);
            builder.error("error at instance group: " + instanceGroup.getGroupName());
            return null;
        };
        org.mockito.Mockito.doAnswer(errorAnswer).when(templateValidatorAndUpdater).validate(any(), any(), any(), any(), any(), any());

        ValidateCloudConfigRequest request = new ValidateCloudConfigRequest(RESOURCE_ID);
        HandlerEvent<ValidateCloudConfigRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> underTest.doAccept(handlerEvent));
        Assertions.assertThat(exception.getMessage())
                .contains("error at instance group: ig1")
                .contains("error at instance group: ig2");
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new RuntimeException("error");
        ValidateCloudConfigRequest request = new ValidateCloudConfigRequest(RESOURCE_ID);
        Event<ValidateCloudConfigRequest> event = new Event<>(request);

        Selectable result = underTest.defaultFailureEvent(RESOURCE_ID, e, event);

        assertEquals("VALIDATE_CLOUD_CONFIG_FAILED_EVENT", result.selector());
        assertEquals(RESOURCE_ID, ((StackFailureEvent) result).getResourceId());
        assertEquals(e, ((StackFailureEvent) result).getException());
    }

    @Test
    void testSelector() {
        assertEquals("VALIDATECLOUDCONFIGREQUEST", underTest.selector());
    }
}