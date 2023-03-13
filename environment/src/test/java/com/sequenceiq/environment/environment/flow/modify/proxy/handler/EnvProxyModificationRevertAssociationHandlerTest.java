package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class EnvProxyModificationRevertAssociationHandlerTest {

    private static final String SELECTOR = "selector";

    private static final long ENV_ID = 1L;

    private static final String PROXY_CONFIG_CRN = "proxy-crn";

    private static final String PREVIOUS_PROXY_CONFIG_CRN = "prev-proxy-crn";

    private static final RuntimeException CAUSE = new RuntimeException("cause");

    private static final EnvironmentStatus ENVIRONMENT_STATUS = EnvironmentStatus.PROXY_CONFIG_MODIFICATION_FAILED;

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private EnvProxyModificationRevertAssociationHandler underTest;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private ProxyConfig previousProxyConfig;

    @Captor
    private ArgumentCaptor<EnvProxyModificationDefaultEvent> eventCaptor;

    private EnvProxyModificationFailedEvent event;

    @BeforeEach
    void setUp() {
        event = createFailedEventWithPreviousProxyCrn(PREVIOUS_PROXY_CONFIG_CRN);
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentService.findById(ENV_ID)).thenReturn(Optional.of(environmentDto));
        lenient().when(proxyConfigService.getByCrn(PREVIOUS_PROXY_CONFIG_CRN)).thenReturn(previousProxyConfig);
    }

    private EnvProxyModificationFailedEvent createFailedEventWithPreviousProxyCrn(String previousProxyConfigCrn) {
        EnvProxyModificationDefaultEvent defaultEvent = EnvProxyModificationDefaultEvent.builder()
                .withSelector(SELECTOR)
                .withResourceId(ENV_ID)
                .withProxyConfigCrn(PROXY_CONFIG_CRN)
                .withPreviousProxyConfigCrn(previousProxyConfigCrn)
                .build();
        return new EnvProxyModificationFailedEvent(defaultEvent, CAUSE, ENVIRONMENT_STATUS);
    }

    @Test
    void revertToPreviousProxy() {
        Event<EnvProxyModificationFailedEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        verify(environmentService).updateProxyConfig(ENV_ID, previousProxyConfig);
        verifySendEvent(wrappedEvent);
    }

    @Test
    void revertToNoProxy() {
        event = createFailedEventWithPreviousProxyCrn(null);
        Event<EnvProxyModificationFailedEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        verify(environmentService).updateProxyConfig(ENV_ID, null);
        verifySendEvent(wrappedEvent);
    }

    @Test
    void updateProxyConfigFailureShouldNotFailHandler() {
        doThrow(new RuntimeException()).when(environmentService).updateProxyConfig(any(), any());
        Event<EnvProxyModificationFailedEvent> wrappedEvent = Event.wrap(event);

        assertThatCode(() -> underTest.accept(wrappedEvent))
                .doesNotThrowAnyException();
        verifySendEvent(wrappedEvent);
    }

    private void verifySendEvent(Event<EnvProxyModificationFailedEvent> wrappedEvent) {
        verify(eventSender).sendEvent(eventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(eventCaptor.getValue())
                .returns(EnvProxyModificationStateSelectors.HANDLE_FAILED_MODIFY_PROXY_EVENT.selector(), Selectable::getSelector);
    }

}
