package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

@ExtendWith(MockitoExtension.class)
class PublicKeyCreationHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String CLOUD_PLATFORM = "platform";

    private static final String LOCATION = "location";

    private static final String PUBLIC_KEY_ID = "publicKeyId";

    private static final String PUBLIC_KEY = "ssh-rsa";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private Event<EnvironmentDto> environmentDtoEvent;

    @Mock
    private Headers headers;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private PublicKeyConnector publicKeyConnector;

    @InjectMocks
    private PublicKeyCreationHandler underTest;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @Captor
    private ArgumentCaptor<Headers> headersArgumentCaptor;

    @BeforeEach
    void setUp() {
        EnvironmentDto eventDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .build();
        when(environmentDtoEvent.getData()).thenReturn(eventDto);
        when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEvent.capture(), any(Headers.class));
    }

    @Test
    void acceptEnvironmentNotFound() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDtoEvent);

        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verifyEnvCreationEvent();
    }

    @Test
    void acceptTestEnvironmentFailure() {
        IllegalStateException error = new IllegalStateException("error");
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(error);

        underTest.accept(environmentDtoEvent);
        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verifyEnvCreationFailedEvent(error);
    }

    @Test
    void acceptTestAwsFailure() {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.publicKey()).thenReturn(publicKeyConnector);
        CloudCredential cloudCredential = new CloudCredential();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(cloudCredential);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(true)));
        IllegalStateException error = new IllegalStateException("error");
        doThrow(error).when(publicKeyConnector).register(any());

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verify(cloudPlatformConnectors).get(any());
        PublicKeyRegisterRequest request = getPublicKeyRegisterRequest(cloudCredential);
        verify(publicKeyConnector).register(request);
        verifyEnvCreationFailedEvent(error);
    }

    @Test
    void acceptManagedKey() {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.publicKey()).thenReturn(publicKeyConnector);
        CloudCredential cloudCredential = new CloudCredential();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(cloudCredential);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(true)));

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verify(cloudPlatformConnectors).get(any());
        PublicKeyRegisterRequest request = getPublicKeyRegisterRequest(cloudCredential);
        verify(publicKeyConnector).register(request);
        verifyEnvCreationEvent();
    }

    @Test
    void acceptNonManagedKey() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(false)));

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verify(publicKeyConnector, never()).unregister(any());
        verifyEnvCreationEvent();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("CREATE_PUBLICKEY_EVENT");
    }

    private Environment createEnvironment(boolean managedPublicKey) {
        Environment env = new Environment();
        env.setId(ENVIRONMENT_ID);
        env.setName(ENVIRONMENT_NAME);
        env.setResourceCrn(ENVIRONMENT_CRN);
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        authentication.setManagedKey(managedPublicKey);
        authentication.setPublicKey(PUBLIC_KEY);
        env.setAuthentication(authentication);
        env.setLocation(LOCATION);
        env.setCloudPlatform(CLOUD_PLATFORM);
        Credential credential = new Credential();
        credential.setCloudPlatform(CLOUD_PLATFORM);
        env.setCredential(credential);
        return env;
    }

    private PublicKeyRegisterRequest getPublicKeyRegisterRequest(CloudCredential cloudCredential) {
        return PublicKeyRegisterRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(cloudCredential)
                .withRegion(LOCATION)
                .withPublicKeyId(String.format("%s-%s", ENVIRONMENT_NAME, ENVIRONMENT_CRN))
                .withPublicKey(PUBLIC_KEY)
                .build();
    }

    private void verifyEnvCreationEvent() {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvCreationEvent.class);

        EnvCreationEvent envCreationEvent = (EnvCreationEvent) event;
        assertThat(envCreationEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envCreationEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envCreationEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envCreationEvent.selector()).isEqualTo(START_FREEIPA_CREATION_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvCreationFailedEvent(Exception exceptionExpected) {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvCreationFailureEvent.class);

        EnvCreationFailureEvent envCreateFailedEvent = (EnvCreationFailureEvent) event;
        assertThat(envCreateFailedEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envCreateFailedEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envCreateFailedEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envCreateFailedEvent.selector()).isEqualTo(FAILED_ENV_CREATION_EVENT.selector());
        assertThat(envCreateFailedEvent.getException()).isSameAs(exceptionExpected);

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }
}
