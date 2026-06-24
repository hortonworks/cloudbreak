package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrotate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
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
import com.sequenceiq.cloudbreak.service.cluster.CertificateExpirationService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.ssh.SshKeyService;
import com.sequenceiq.cloudbreak.view.ClusterView;
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

    @Mock
    private CertificateExpirationService certExpirationService;

    @InjectMocks
    private ClusterHostCertificatesRotationHandler underTest;

    @Test
    void testRotateWhenGovAndCMCARotationIncluded() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getPlatformVariant()).thenReturn(CloudConstants.AWS_NATIVE_GOV);
        when(stackDto.getAllFunctioningNodes()).thenReturn(
                Set.of(new Node(null, null, null, null, "host1", null)));
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(false);
        doNothing().when(saltService).executeSaltState(any(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.ALL))));

        verify(saltService, never()).executeSaltStateOnPrimaryGateway(any(), any());
        ArgumentCaptor<Set<String>> targetCaptor = ArgumentCaptor.forClass(Set.class);
        verify(saltService).executeSaltState(any(), targetCaptor.capture(),
                eq(List.of("cloudera.manager.rotate.host-cert-manual-renewal")));
        assertEquals(Set.of("host1"), targetCaptor.getValue());
        assertEquals(ClusterHostCertificatesRotationSuccess.class, result.getClass());
        verifyNoInteractions(sshKeyService, clusterComponentConfigProvider, loadBalancerSANProvider);
    }

    @Test
    void testRotateFailureWhenGovAndCMCARotationIncluded() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getPlatformVariant()).thenReturn(CloudConstants.AWS_NATIVE_GOV);
        when(stackDto.getAllFunctioningNodes()).thenReturn(
                Set.of(new Node(null, null, null, null, "host1", null)));
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(false);
        doThrow(CloudbreakServiceException.class).when(saltService).executeSaltState(any(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.ALL))));

        assertEquals(ClusterCertificatesRotationFailed.class, result.getClass());
        verifyNoInteractions(sshKeyService, clusterComponentConfigProvider, loadBalancerSANProvider);
    }

    @Test
    void testRotateWhenCertFullyExpired() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(
                new Node(null, null, null, null, "host1", null),
                new Node(null, null, null, null, "host2", null)));
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(true);
        doNothing().when(saltService).executeSaltStateOnPrimaryGateway(any(), any());
        doNothing().when(saltService).executeSaltState(any(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.HOST_CERTS))));

        verify(saltService).executeSaltStateOnPrimaryGateway(eq(stackDto),
                eq(List.of("cloudera.manager.rotate.host-cert-expired-token-generation")));
        ArgumentCaptor<Set<String>> targetCaptor = ArgumentCaptor.forClass(Set.class);
        verify(saltService).executeSaltState(eq(stackDto), targetCaptor.capture(),
                eq(List.of("cloudera.manager.rotate.host-cert-manual-renewal")));
        assertEquals(Set.of("host1", "host2"), targetCaptor.getValue());
        assertEquals(ClusterHostCertificatesRotationSuccess.class, result.getClass());
        verifyNoInteractions(sshKeyService, clusterComponentConfigProvider, loadBalancerSANProvider);
    }

    @Test
    void testRotateWhenValidateCertificateFullyExpiredTakesSaltPath() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getPlatformVariant()).thenReturn("AWS");
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(
                new Node(null, null, null, null, "host1", null),
                new Node(null, null, null, null, "host2", null)));
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(true);
        doNothing().when(saltService).executeSaltStateOnPrimaryGateway(any(), any());
        doNothing().when(saltService).executeSaltState(any(), any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.HOST_CERTS))));

        verify(saltService).executeSaltStateOnPrimaryGateway(eq(stackDto),
                eq(List.of("cloudera.manager.rotate.host-cert-expired-token-generation")));
        ArgumentCaptor<Set<String>> targetCaptor = ArgumentCaptor.forClass(Set.class);
        verify(saltService).executeSaltState(eq(stackDto), targetCaptor.capture(),
                eq(List.of("cloudera.manager.rotate.host-cert-manual-renewal")));
        assertEquals(Set.of("host1", "host2"), targetCaptor.getValue());
        assertEquals(ClusterHostCertificatesRotationSuccess.class, result.getClass());
        verifyNoInteractions(sshKeyService, clusterComponentConfigProvider, loadBalancerSANProvider);
    }

    @Test
    void testRotateAllHostsHealthyUsesCmApi() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        com.sequenceiq.cloudbreak.view.StackView stackView = mock(com.sequenceiq.cloudbreak.view.StackView.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getPlatformVariant()).thenReturn("AWS");
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackView.getId()).thenReturn(1L);
        when(clusterView.getId()).thenReturn(1L);
        when(stackDto.getBlueprint()).thenReturn(mock(com.sequenceiq.cloudbreak.domain.Blueprint.class));
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(false);
        when(apiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(loadBalancerSANProvider.getLoadBalancerSAN(any(), any())).thenReturn(Optional.empty());
        com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo repo =
                mock(com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo.class);
        when(repo.getVersion()).thenReturn("7.11.0");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(1L)).thenReturn(repo);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.HOST_CERTS))));

        verify(saltService, never()).executeSaltStateOnPrimaryGateway(any(), any());
        verify(saltService, never()).executeSaltState(any(), any(), any());
        verify(clusterApi).rotateHostCertificates(null, null, null);
        assertEquals(ClusterHostCertificatesRotationSuccess.class, result.getClass());
    }

    @Test
    void testRotateFailureWhenCertExpiredAndTokenGenerationFails() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        when(certExpirationService.validateCertificateFullyExpired(stackDto)).thenReturn(true);
        doThrow(new CloudbreakServiceException("salt failed"))
                .when(saltService).executeSaltStateOnPrimaryGateway(any(), any());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(
                new ClusterHostCertificatesRotationRequest(1L, CertificateRotationType.HOST_CERTS))));

        assertEquals(ClusterCertificatesRotationFailed.class, result.getClass());
        verify(saltService, never()).executeSaltState(any(), any(), any());
    }
}
