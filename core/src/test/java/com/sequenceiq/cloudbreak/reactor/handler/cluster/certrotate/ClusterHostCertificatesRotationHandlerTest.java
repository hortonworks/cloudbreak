package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrotate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificatesRotationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationSuccess;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.san.LoadBalancerSANProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.ssh.SshKeyService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterHostCertificatesRotationHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors apiConnectors;

    @Mock
    private SshKeyService sshKeyService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private LoadBalancerSANProvider loadBalancerSANProvider;

    @Mock
    private SecretRotationSaltService saltService;

    @InjectMocks
    private ClusterHostCertificatesRotationHandler underTest;

    @Test
    void testRotateWhenGovAndCMCARotationIncluded() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(new Node(null, null, null, null, "host1", null)));
        when(stackDto.getPlatformVariant()).thenReturn(CloudConstants.AWS_NATIVE_GOV);
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        doNothing().when(saltService).executeSaltState(any(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.ALL))));

        ArgumentCaptor<Set<String>> targetCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List<String>> statesCaptor = ArgumentCaptor.forClass(List.class);
        verify(saltService).executeSaltState(any(), targetCaptor.capture(), statesCaptor.capture());
        assertEquals(targetCaptor.getValue(), Set.of("host1"));
        assertEquals(statesCaptor.getValue(), List.of("cloudera.manager.rotate.host-cert-manual-renewal"));
        assertEquals(ClusterHostCertificatesRotationSuccess.class, result.getClass());
        verifyNoInteractions(apiConnectors, sshKeyService, clusterComponentConfigProvider, loadBalancerSANProvider);
    }

    @Test
    void testRotateFailureWhenGovAndCMCARotationIncluded() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(new Node(null, null, null, null, "host1", null)));
        when(stackDto.getPlatformVariant()).thenReturn(CloudConstants.AWS_NATIVE_GOV);
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        doThrow(CloudbreakServiceException.class).when(saltService).executeSaltState(any(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.ALL))));

        ArgumentCaptor<Set<String>> targetCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List<String>> statesCaptor = ArgumentCaptor.forClass(List.class);
        verify(saltService).executeSaltState(any(), targetCaptor.capture(), statesCaptor.capture());
        assertEquals(targetCaptor.getValue(), Set.of("host1"));
        assertEquals(statesCaptor.getValue(), List.of("cloudera.manager.rotate.host-cert-manual-renewal"));
        assertEquals(ClusterCertificatesRotationFailed.class, result.getClass());
        verifyNoInteractions(apiConnectors, sshKeyService, clusterComponentConfigProvider, loadBalancerSANProvider);
    }
}
