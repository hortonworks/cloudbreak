package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class PublicKeyDeleteHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String USER_CRN = "userCrn";

    private static final String PUBLIC_KEY_ID = "publicKeyId";

    @Mock
    private EventSender eventSender;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @Mock
    private HandlerExceptionProcessor handlerExceptionProcessor;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private EnvironmentResourceService environmentResourceService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Event.Headers headers;

    @InjectMocks
    private PublicKeyDeleteHandler underTest;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @Captor
    private ArgumentCaptor<Event.Headers> headersArgumentCaptor;

    private EnvironmentDeletionDto environmentDeletionDto;

    @BeforeEach
    void setUp() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .build();
        environmentDeletionDto = EnvironmentDeletionDto
                .builder()
                .withId(ENVIRONMENT_ID)
                .withForceDelete(false)
                .withEnvironmentDto(environmentDto)
                .build();
        lenient().when(environmentDtoEvent.getData()).thenReturn(environmentDeletionDto);
        lenient().when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        lenient().doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEvent.capture(), any(Event.Headers.class));
    }

    static Object[][] acceptTestWhenNoManagedKeyDataProvider() {
        return new Object[][]{
                // managedKey, publicKeyId
                {false, null},
                {true, null},
                {false, PUBLIC_KEY_ID},
        };
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("acceptTestWhenNoManagedKeyDataProvider")
    void acceptTestWhenNoManagedKey(boolean managedKey, String publicKeyId) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withPublicKeyId(publicKeyId)
                .withManagedKey(managedKey)
                .build();
        environmentDto.setAuthentication(authenticationDto);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        verify(eventSenderService, never())
                .sendEventAndNotification(eq(environmentDto), eq(USER_CRN), eq(ResourceEvent.ENVIRONMENT_SSH_DELETION_SKIPPED), any(List.class));
        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptTestNotificationFailure() {
        IllegalStateException error = new IllegalStateException("error");
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withPublicKeyId(PUBLIC_KEY_ID)
                .withManagedKey(true)
                .build();
        environmentDto.setAuthentication(authenticationDto);
        doNothing().when(environmentResourceService).deletePublicKey(any());
        when(environmentService.findEnvironmentByIdOrThrow(any())).thenReturn(new Environment());
        doThrow(error).when(eventSenderService)
                .sendEventAndNotification(environmentDto, USER_CRN, ResourceEvent.ENVIRONMENT_SSH_DELETION_APPLIED, List.of(PUBLIC_KEY_ID));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        verify(handlerExceptionProcessor).handle(any(HandlerFailureConjoiner.class), any(Logger.class), eq(eventSender), eq(DELETE_PUBLICKEY_EVENT.selector()));
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DELETE_PUBLICKEY_EVENT");
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

}
