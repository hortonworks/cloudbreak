package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_S3GUARD_TABLE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

@ExtendWith(MockitoExtension.class)
class S3GuardTableDeleteHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String CLOUD_PLATFORM = "platform";

    private static final String LOCATION = "location";

    private static final String DYNAMO_TABLE_NAME = "tableName";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @InjectMocks
    private S3GuardTableDeleteHandler underTest;

    @Mock
    private Event<EnvironmentDto> environmentDtoEvent;

    @Mock
    private Headers headers;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private NoSqlConnector noSql;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Headers> headersArgumentCaptor;

    @BeforeEach
    void setUp() {
        when(environmentDtoEvent.getData()).thenReturn(createEnvironmentDto());
        when(environmentDtoEvent.getHeaders()).thenReturn(headers);
    }

    @Test
    void acceptTestNoEnvironment() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDtoEvent);

        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptTestEnvironmentNotAwsParameters() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(new MockParameters())));

        underTest.accept(environmentDtoEvent);

        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptTestEnvironmentPreExistingTable() {
        AwsParameters awsParameters = getAwsParameters(S3GuardTableCreation.USE_EXISTING);

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(awsParameters)));

        underTest.accept(environmentDtoEvent);

        verify(cloudPlatformConnectors, never()).get(any());
        verify(cloudPlatformConnectors, never()).get(any(), any());
        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptTestEnvironmentAwsFailure() {
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(cloudConnector);
        when(cloudConnector.noSql()).thenReturn(noSql);
        CloudCredential cloudCredential = new CloudCredential();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(cloudCredential);

        AwsParameters awsParameters = getAwsParameters(S3GuardTableCreation.CREATE_NEW);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(awsParameters)));

        Exception exception = new CloudConnectorException(String.format("Cannot delete NoSQL table %s. "
                + "Provider error message: %s", DYNAMO_TABLE_NAME, "errorMessage"));
        when(noSql.deleteNoSqlTable(any(NoSqlTableDeleteRequest.class))).thenThrow(exception);

        underTest.accept(environmentDtoEvent);

        NoSqlTableDeleteRequest request = getNoSqlTableDeleteRequest(cloudCredential);
        verify(cloudPlatformConnectors).get(any(), any());
        verify(noSql).deleteNoSqlTable(request);
        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteFailedEvent(exception);
    }

    @Test
    void acceptTestEnvironmentSuccess() {
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(cloudConnector);
        when(cloudConnector.noSql()).thenReturn(noSql);
        CloudCredential cloudCredential = new CloudCredential();
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(cloudCredential);

        AwsParameters awsParameters = getAwsParameters(S3GuardTableCreation.CREATE_NEW);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(awsParameters)));
        when(noSql.deleteNoSqlTable(any(NoSqlTableDeleteRequest.class))).thenReturn(NoSqlTableDeleteResponse.builder().withStatus(ResponseStatus.OK).build());

        underTest.accept(environmentDtoEvent);

        NoSqlTableDeleteRequest request = getNoSqlTableDeleteRequest(cloudCredential);
        verify(cloudPlatformConnectors).get(any(), any());
        verify(noSql).deleteNoSqlTable(request);
        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertEquals(DELETE_S3GUARD_TABLE_EVENT.selector(), underTest.selector());
    }

    private EnvironmentDto createEnvironmentDto() {
        return EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withName(ENVIRONMENT_NAME)
                .withResourceCrn(ENVIRONMENT_CRN)
                .build();
    }

    private Environment createEnvironment(BaseParameters parameters) {
        Environment env = new Environment();
        env.setId(ENVIRONMENT_ID);
        env.setResourceCrn(ENVIRONMENT_CRN);
        env.setParameters(parameters);
        env.setLocation(LOCATION);
        Credential credential = new Credential();
        credential.setCloudPlatform(CLOUD_PLATFORM);
        env.setCredential(credential);
        return env;
    }

    private void verifyEnvDeleteEvent() {
        BaseNamedFlowEvent event = eventArgumentCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteEvent.class);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) event;
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.selector()).isEqualTo(FINISH_ENV_DELETE_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvDeleteFailedEvent(Exception exceptionExpected) {
        BaseNamedFlowEvent event = eventArgumentCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteFailedEvent.class);

        EnvDeleteFailedEvent envDeleteFailedEvent = (EnvDeleteFailedEvent) event;
        assertThat(envDeleteFailedEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteFailedEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteFailedEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteFailedEvent.selector()).isEqualTo(FAILED_ENV_DELETE_EVENT.selector());
        assertThat(envDeleteFailedEvent.getException()).isSameAs(exceptionExpected);

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private NoSqlTableDeleteRequest getNoSqlTableDeleteRequest(CloudCredential cloudCredential) {
        return NoSqlTableDeleteRequest.builder()
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(cloudCredential)
                .withRegion(LOCATION)
                .withTableName(DYNAMO_TABLE_NAME)
                .build();
    }

    private AwsParameters getAwsParameters(S3GuardTableCreation useExisting) {
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setS3guardTableCreation(useExisting);
        awsParameters.setS3guardTableName(DYNAMO_TABLE_NAME);
        return awsParameters;
    }

    static class MockParameters extends BaseParameters {
    }
}
