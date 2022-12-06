package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

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
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class ClusterDbCertRotationHandlerTest {

    private static final Long CLUSTER_ID = 12L;

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private ClusterDbCertRotationHandler underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Mock
    private EventBus eventBus;

    @Test
    public void testDoAccept() {
        Set<String> rootSslCerts = Set.of("any-db-cert");
        HandlerEvent<ClusterDbCertRotationRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(new ClusterDbCertRotationRequest(STACK_ID));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(clusterView);
        RedbeamsDbCertificateProvider.RedbeamsDbSslDetails redbeamsSslDetails = new RedbeamsDbCertificateProvider.RedbeamsDbSslDetails(rootSslCerts, true);
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenReturn(redbeamsSslDetails);
        underTest.doAccept(handlerEvent);
        verify(clusterService).updateDbSslCert(12L, redbeamsSslDetails);
    }

    @Test
    public void testAcceptWhenError() {
        Event<ClusterDbCertRotationRequest> event = mock(Event.class);
        when(event.getData()).thenReturn(new ClusterDbCertRotationRequest(STACK_ID));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(event.getHeaders()).thenReturn(new Event.Headers());
        when(dbCertificateProvider.getRelatedSslCerts(stackDto)).thenThrow(new RuntimeException("any-error"));
        ArgumentCaptor<Event<ClusterDbCertRotationResult>> failureEventCapture = ArgumentCaptor.forClass(Event.class);
        underTest.accept(event);
        verify(clusterService, never()).updateDbSslCert(any(), any());
        verify(eventBus).notify(eq("CLUSTERDBCERTROTATIONRESULT_ERROR"), failureEventCapture.capture());
        Event<ClusterDbCertRotationResult> failureEvent = failureEventCapture.getValue();
        Assertions.assertThat(failureEvent.getData().getErrorDetails().getMessage()).isEqualTo("any-error");
        Assertions.assertThat(failureEvent.getData().getStatusReason()).isEqualTo("Cannot rotate the DB root CERT on the cluster");
    }
}
