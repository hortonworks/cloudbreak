package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class StartAmbariServicesHandlerTest {

    private static final String EXCEPTION_MESSAGE = "Salt error";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private StartAmbariServicesHandler underTest;

    @Mock
    private EventBus eventBus;

    @Mock
    private ClusterServiceRunner clusterServiceRunner;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stack;

    @Mock
    private ClusterApi clusterApi;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testAcceptWhenStartAmbariHandlerSucceeds(boolean defaultClusterManagerAuth) throws ClusterClientInitException, CloudbreakException {

        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        when(stack.getClusterManagerIp()).thenReturn("ip");
        when(clusterApiConnectors.getConnector(stack, "ip")).thenReturn(clusterApi);

        underTest.accept(getEvent(defaultClusterManagerAuth));

        verify(clusterApi).waitForServer(eq(defaultClusterManagerAuth));
        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(Object.class), resultCaptor.capture());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StartClusterManagerServicesSuccess.class, resultEvent.getData().getClass());
        StartClusterManagerServicesSuccess result = (StartClusterManagerServicesSuccess) resultEvent.getData();
        assertEquals(STACK_ID, result.getResourceId());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testAcceptWhenExceptionThenFailure(boolean defaultClusterManagerAuth) {
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        doThrow(new CloudbreakServiceException(EXCEPTION_MESSAGE)).when(clusterServiceRunner).runAmbariServices(stack);
        underTest.accept(getEvent(defaultClusterManagerAuth));

        ArgumentCaptor<Event> resultCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(Object.class), resultCaptor.capture());

        assertEquals(1, resultCaptor.getAllValues().size());
        Event resultEvent = resultCaptor.getValue();
        assertEquals(StartAmbariServicesFailed.class, resultEvent.getData().getClass());
        StartAmbariServicesFailed result = (StartAmbariServicesFailed) resultEvent.getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(EXCEPTION_MESSAGE, result.getException().getMessage());
    }

    private Event<StartAmbariServicesRequest> getEvent(boolean defaultClusterManagerAuth) {
        StartAmbariServicesRequest startAmbariServicesRequest =
                new StartAmbariServicesRequest(STACK_ID, defaultClusterManagerAuth);
        Event<StartAmbariServicesRequest> handlerEvent = mock(Event.class);
        when(handlerEvent.getData()).thenReturn(startAmbariServicesRequest);
        return handlerEvent;
    }
}