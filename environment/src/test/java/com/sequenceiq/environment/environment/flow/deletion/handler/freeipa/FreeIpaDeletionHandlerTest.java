package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;


import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class FreeIpaDeletionHandlerTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String PARENT_ENVIRONMENT_CRN = "childEnvironmentCrn";

    private static final Long CHILD_ENVIRONMENT_ID = 1L;

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private PollingService<FreeIpaPollerObject> freeIpaPollingService;

    @InjectMocks
    private FreeIpaDeletionHandler victim;

    @Test
    public void shouldDetachChildEnvironmentIfParentExists() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(CHILD_ENVIRONMENT_ID);

        when(environmentService.findEnvironmentById(CHILD_ENVIRONMENT_ID)).thenReturn(of(anEnvironmentWithParent()));
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(of(new DescribeFreeIpaResponse()));

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<DetachChildEnvironmentRequest> detachChildEnvironmentRequestArgumentCaptor
                = ArgumentCaptor.forClass(DetachChildEnvironmentRequest.class);
        verify(freeIpaService).detachChildEnvironment(detachChildEnvironmentRequestArgumentCaptor.capture());
        verifyNoMoreInteractions(freeIpaService);
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));

        assertEquals(PARENT_ENVIRONMENT_CRN, detachChildEnvironmentRequestArgumentCaptor.getValue().getParentEnvironmentCrn());
        assertEquals(ENVIRONMENT_CRN, detachChildEnvironmentRequestArgumentCaptor.getValue().getChildEnvironmentCrn());
    }

    @Test
    public void shouldDeleteFreeIpaIfParentDoesNotExist() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(CHILD_ENVIRONMENT_ID);
        Pair<PollingResult, Exception> pollingResult = new ImmutablePair<>(PollingResult.SUCCESS, null);

        when(environmentService.findEnvironmentById(CHILD_ENVIRONMENT_ID)).thenReturn(of(anEnvironmentWithoutParent(Boolean.TRUE)));
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(of(new DescribeFreeIpaResponse()));
        when(freeIpaPollingService.pollWithTimeout(any(),
                any(),
                Mockito.eq(FreeIpaDeletionRetrievalTask.FREEIPA_RETRYING_INTERVAL),
                Mockito.eq(FreeIpaDeletionRetrievalTask.FREEIPA_RETRYING_COUNT),
                Mockito.eq(1))).thenReturn(pollingResult);

        victim.accept(new Event<>(environmentDto));

        verify(freeIpaService).delete(ENVIRONMENT_CRN);
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));
        verifyNoMoreInteractions(freeIpaService);
    }

    @Test
    public void shouldNotCallFreeIpaServiceIfFreeIpaCreationIsDisabled() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(CHILD_ENVIRONMENT_ID);

        when(environmentService.findEnvironmentById(CHILD_ENVIRONMENT_ID)).thenReturn(of(anEnvironmentWithoutParent(Boolean.FALSE)));

        victim.accept(new Event<>(environmentDto));

        verifyNoMoreInteractions(freeIpaService);
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));
    }

    private Environment anEnvironmentWithParent() {
        Environment environment = new Environment();
        Environment parentEnvironment = new Environment();
        parentEnvironment.setResourceCrn(PARENT_ENVIRONMENT_CRN);

        environment.setParentEnvironment(parentEnvironment);
        environment.setResourceCrn(ENVIRONMENT_CRN);
        environment.setCreateFreeIpa(Boolean.TRUE);

        return environment;
    }

    private Environment anEnvironmentWithoutParent(boolean createFreeIpa) {
        Environment environment = new Environment();
        environment.setResourceCrn(ENVIRONMENT_CRN);
        environment.setCreateFreeIpa(createFreeIpa);

        return environment;
    }
}