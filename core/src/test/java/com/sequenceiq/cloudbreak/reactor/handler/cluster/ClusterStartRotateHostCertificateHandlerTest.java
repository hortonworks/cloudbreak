package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRotateHostCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRotateHostCertificateResult;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.service.cluster.CertificateExpirationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterStartRotateHostCertificateHandlerTest {

    private static final Long STACK_ID = 42L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CertificateExpirationService certExpirationService;

    @Mock
    private SecretRotationSaltService saltService;

    @InjectMocks
    private ClusterStartRotateHostCertificateHandler underTest;

    @Test
    void testRotationNeededExecutesSaltStates() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getName()).thenReturn("test-cluster");
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(
                new Node(null, null, null, null, "host1", null),
                new Node(null, null, null, null, "host2", null)));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(true);

        Selectable result = underTest.doAccept(handlerEvent());

        assertInstanceOf(ClusterStartRotateHostCertificateResult.class, result);
        assertEquals(EventStatus.OK, ((ClusterStartRotateHostCertificateResult) result).getStatus());
        verify(saltService).executeSaltStateOnPrimaryGateway(stackDto,
                java.util.List.of("cloudera.manager.rotate.host-cert-expired-token-generation"));
        verify(saltService).executeSaltState(eq(stackDto), any(), eq(java.util.List.of("cloudera.manager.rotate.host-cert-manual-renewal")));
    }

    @Test
    void testRotationNotNeededSkipsSaltStates() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getName()).thenReturn("test-cluster");
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(false);

        Selectable result = underTest.doAccept(handlerEvent());

        assertInstanceOf(ClusterStartRotateHostCertificateResult.class, result);
        assertEquals(EventStatus.OK, ((ClusterStartRotateHostCertificateResult) result).getStatus());
        verify(saltService, never()).executeSaltStateOnPrimaryGateway(any(), any());
        verify(saltService, never()).executeSaltState(any(), any(), any());
    }

    @Test
    void testDefaultFailureEventReturnsFailureResult() {
        ClusterStartRotateHostCertificateRequest request = new ClusterStartRotateHostCertificateRequest(STACK_ID);
        Exception cause = new RuntimeException("salt failed");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, cause, new Event<>(request));

        assertInstanceOf(ClusterStartRotateHostCertificateResult.class, result);
        assertEquals(EventStatus.FAILED, ((ClusterStartRotateHostCertificateResult) result).getStatus());
        assertEquals(STACK_ID, result.getResourceId());
    }

    @Test
    void testCertExpirationCheckFailureSkipsRotation() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getName()).thenReturn("test-cluster");
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto))
                .thenThrow(new CloudbreakOrchestratorFailedException("check failed"));

        Selectable result = underTest.doAccept(handlerEvent());

        assertInstanceOf(ClusterStartRotateHostCertificateResult.class, result);
        assertEquals(EventStatus.OK, ((ClusterStartRotateHostCertificateResult) result).getStatus());
        verify(saltService, never()).executeSaltStateOnPrimaryGateway(any(), any());
        verify(saltService, never()).executeSaltState(any(), any(), any());
    }

    private HandlerEvent<ClusterStartRotateHostCertificateRequest> handlerEvent() {
        return new HandlerEvent<>(new Event<>(new ClusterStartRotateHostCertificateRequest(STACK_ID)));
    }
}
