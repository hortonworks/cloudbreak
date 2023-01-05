package com.sequenceiq.environment.environment.flow.modify.proxy.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.datahub.DatahubModifyProxyConfigPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class ProxyModificationOnDatahubsHandlerTest {

    private static final String SELECTOR = "selector";

    private static final long ENV_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    private static final String NAME = "name";

    private static final String PROXY_CRN = "proxy-crn";

    private static final String PREV_PROXY_CRN = "prev-proxy-crn";

    @Mock
    private DatahubModifyProxyConfigPollerService datahubModifyProxyConfigPollerService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private ProxyModificationOnDatahubsHandler underTest;

    @Captor
    private ArgumentCaptor<EnvProxyModificationDefaultEvent> defaultEventCaptor;

    @Captor
    private ArgumentCaptor<EnvProxyModificationFailedEvent> failedEventCaptor;

    private EnvProxyModificationDefaultEvent event;

    @BeforeEach
    void setUp() {
        event = EnvProxyModificationDefaultEvent.builder()
                .withSelector(SELECTOR)
                .withResourceId(ENV_ID)
                .withResourceCrn(ENV_CRN)
                .withProxyConfigCrn(PROXY_CRN)
                .withPreviousProxyConfigCrn(PREV_PROXY_CRN)
                .build();
    }

    @Test
    void failure() {
        RuntimeException cause = new RuntimeException("cause");
        doThrow(cause).when(datahubModifyProxyConfigPollerService).modifyProxyOnAttachedDatahubs(ENV_ID, ENV_CRN, PREV_PROXY_CRN);
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        verify(eventSender).sendEvent(failedEventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(failedEventCaptor.getValue())
                .returns(EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_FAILED, EnvProxyModificationFailedEvent::getEnvironmentStatus)
                .returns(cause, EnvProxyModificationFailedEvent::getException);
    }

    @Test
    void success() {
        Event<EnvProxyModificationDefaultEvent> wrappedEvent = Event.wrap(event);

        underTest.accept(wrappedEvent);

        verify(datahubModifyProxyConfigPollerService).modifyProxyOnAttachedDatahubs(ENV_ID, ENV_CRN, PREV_PROXY_CRN);
        verify(eventSender).sendEvent(defaultEventCaptor.capture(), eq(wrappedEvent.getHeaders()));
        assertThat(defaultEventCaptor.getValue())
                .returns(EnvProxyModificationStateSelectors.FINISH_MODIFY_PROXY_EVENT.selector(), EnvProxyModificationDefaultEvent::selector);
    }

}
