package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterDbCertRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterDbCertRotationResult;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class ClusterDbCertRotationHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private ClusterDbCertRotationHandler underTest;

    @Mock
    private DatabaseSslService databaseSslService;

    // TODO add new dependencies

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    @Mock
    private EventBus eventBus;

    @Test
    public void testDoAccept() {
        HandlerEvent<ClusterDbCertRotationRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(new ClusterDbCertRotationRequest(STACK_ID));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        underTest.doAccept(handlerEvent);

        verify(databaseSslService).getDbSslDetailsForRotationAndUpdateInCluster(stackDto);
    }

    @Test
    public void testAcceptWhenError() {
        Event<ClusterDbCertRotationRequest> event = mock(Event.class);
        when(event.getData()).thenReturn(new ClusterDbCertRotationRequest(STACK_ID));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(event.getHeaders()).thenReturn(new Event.Headers());
        when(databaseSslService.getDbSslDetailsForRotationAndUpdateInCluster(stackDto)).thenThrow(new RuntimeException("any-error"));
        ArgumentCaptor<Event<ClusterDbCertRotationResult>> failureEventCapture = ArgumentCaptor.forClass(Event.class);

        underTest.accept(event);

        verify(eventBus).notify(eq("CLUSTERDBCERTROTATIONRESULT_ERROR"), failureEventCapture.capture());
        Event<ClusterDbCertRotationResult> failureEvent = failureEventCapture.getValue();
        Assertions.assertThat(failureEvent.getData().getErrorDetails().getMessage()).isEqualTo("any-error");
        Assertions.assertThat(failureEvent.getData().getStatusReason()).isEqualTo("Cannot rotate the DB root CERT on the cluster");
    }

}
