package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;
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
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

@ExtendWith(MockitoExtension.class)
class PublicKeyDeleteHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String CLOUD_PLATFORM = "platform";

    private static final String LOCATION = "location";

    private static final String DYNAMO_TABLE_NAME = "tableName";

    private static final String PUBLIC_KEY_ID = "publicKeyId";

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
    private PublicKeyDeleteHandler underTest;

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
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptTestEnvironmentFailure() {
        IllegalStateException error = new IllegalStateException("error");
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(error);

        underTest.accept(environmentDtoEvent);
        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verifyEnvDeleteFailedEvent(error);
    }

    @Test
    void acceptTestAwsFailure() {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.publicKey()).thenReturn(publicKeyConnector);
        CloudCredential cloudCredential = new CloudCredential();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(cloudCredential);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(true)));
        IllegalStateException error = new IllegalStateException("error");
        doThrow(error).when(publicKeyConnector).unregister(any());

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verify(cloudPlatformConnectors).get(any());
        PublicKeyUnregisterRequest request = getPublicKeyUnregisterRequest(cloudCredential);
        verify(publicKeyConnector).unregister(request);
        verifyEnvDeleteFailedEvent(error);
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
        PublicKeyUnregisterRequest request = getPublicKeyUnregisterRequest(cloudCredential);
        verify(publicKeyConnector).unregister(request);
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptNonManagedKey() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(false)));

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verify(publicKeyConnector, never()).unregister(any());
        verifyEnvDeleteEvent();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DELETE_PUBLICKEY_EVENT");
    }

    private Environment createEnvironment(boolean managedPublicKey) {
        Environment env = new Environment();
        env.setId(ENVIRONMENT_ID);
        env.setResourceCrn(ENVIRONMENT_CRN);
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        authentication.setManagedKey(managedPublicKey);
        authentication.setPublicKeyId(PUBLIC_KEY_ID);
        env.setAuthentication(authentication);
        env.setLocation(LOCATION);
        env.setCloudPlatform(CLOUD_PLATFORM);
        Credential credential = new Credential();
        credential.setCloudPlatform(CLOUD_PLATFORM);
        env.setCredential(credential);
        return env;
    }

    private PublicKeyUnregisterRequest getPublicKeyUnregisterRequest(CloudCredential cloudCredential) {
        return PublicKeyUnregisterRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(cloudCredential)
                .withRegion(LOCATION)
                .withPublicKeyId(PUBLIC_KEY_ID)
                .build();
    }

    private void verifyEnvDeleteEvent() {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvDeleteEvent.class);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) event;
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.selector()).isEqualTo(START_NETWORK_DELETE_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvDeleteFailedEvent(Exception exceptionExpected) {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvDeleteFailedEvent.class);

        EnvDeleteFailedEvent envDeleteFailedEvent = (EnvDeleteFailedEvent) event;
        assertThat(envDeleteFailedEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteFailedEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteFailedEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteFailedEvent.selector()).isEqualTo(FAILED_ENV_DELETE_EVENT.selector());
        assertThat(envDeleteFailedEvent.getException()).isSameAs(exceptionExpected);

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }
}
