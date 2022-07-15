package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
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

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private RotateSaltPasswordHandler underTest;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Event<?>> eventCaptor;

    @Mock
    private StackDto stack;

    private HandlerEvent<RotateSaltPasswordRequest> event;

    @BeforeEach
    void setUp() {
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        event = new HandlerEvent<>(new Event<>(new RotateSaltPasswordRequest(STACK_ID)));
    }

    @Test
    void testFailure() throws CloudbreakOrchestratorException {
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("error");
        doThrow(exception).when(clusterBootstrapper).rotateSaltPassword(stack);

        Selectable result = underTest.doAccept(event);

        assertThat(result)
                .isInstanceOf(RotateSaltPasswordFailureResponse.class)
                .extracting(RotateSaltPasswordFailureResponse.class::cast)
                .returns(STACK_ID, StackEvent::getResourceId)
                .returns(exception, StackFailureEvent::getException);
        verify(clusterBootstrapper).rotateSaltPassword(stack);
    }

    @Test
    void testSuccess() throws CloudbreakOrchestratorException {
        Selectable result = underTest.doAccept(event);

        assertThat(result)
                .isInstanceOf(RotateSaltPasswordSuccessResponse.class)
                .extracting(RotateSaltPasswordSuccessResponse.class::cast)
                .returns(STACK_ID, StackEvent::getResourceId);
        verify(clusterBootstrapper).rotateSaltPassword(stack);
    }

}
