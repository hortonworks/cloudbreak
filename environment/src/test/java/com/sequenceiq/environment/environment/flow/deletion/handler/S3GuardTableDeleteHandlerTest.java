package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_S3GUARD_TABLE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_CLUSTER_DEFINITION_CLEANUP_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

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
    private HandlerExceptionProcessor mockExceptionProcessor;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @InjectMocks
    private S3GuardTableDeleteHandler underTest;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @Mock
    private Event.Headers headers;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private NoSqlConnector noSql;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event.Headers> headersArgumentCaptor;

    @BeforeEach
    void setUp() {
        when(environmentDtoEvent.getData()).thenReturn(createEnvironmentDto());
        lenient().when(environmentDtoEvent.getHeaders()).thenReturn(headers);
    }

    @Test
    void acceptTestEnvironmentSuccess() {
        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertEquals(DELETE_S3GUARD_TABLE_EVENT.selector(), underTest.selector());
    }

    private EnvironmentDeletionDto createEnvironmentDto() {
        EnvironmentDto build = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withName(ENVIRONMENT_NAME)
                .withResourceCrn(ENVIRONMENT_CRN)
                .build();

        return EnvironmentDeletionDto
                .builder()
                .withId(ENVIRONMENT_ID)
                .withForceDelete(false)
                .withEnvironmentDto(build)
                .build();
    }

    private void verifyEnvDeleteEvent() {
        BaseNamedFlowEvent event = eventArgumentCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteEvent.class);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) event;
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.selector()).isEqualTo(START_CLUSTER_DEFINITION_CLEANUP_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    static class MockParameters extends BaseParameters {
    }
}
