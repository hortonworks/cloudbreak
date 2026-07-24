package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.common.api.type.CertExpirationState;

@ExtendWith(MockitoExtension.class)
class CertificateExpirationServiceTest {

    @Mock
    private SecretRotationSaltService saltService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private ClusterApiConnectors apiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private ExtendedHostStatuses extendedHostStatuses;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @InjectMocks
    private CertificateExpirationService underTest;

    @Test
    void shouldReturnTrueWhenCertExpiredWithZeroDays() {
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.HOST_CERT_EXPIRING);
        when(clusterView.getCertExpirationDetails()).thenReturn(
                "[master0]: Certificate health in Cloudera Manager is BAD: "
                        + "Certificate of Cloudera Manager Agent will expire within 0 days. Critical threshold: 7.");

        assertTrue(underTest.isCertFullyExpired(clusterView));
    }

    @Test
    void shouldReturnFalseWhenCertExpiringButDaysRemaining() {
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.HOST_CERT_EXPIRING);
        when(clusterView.getCertExpirationDetails()).thenReturn(
                "[master0]: Certificate health in Cloudera Manager is CONCERNING: "
                        + "Certificate of Cloudera Manager Agent will expire within 5 days. Critical threshold: 7.");

        assertFalse(underTest.isCertFullyExpired(clusterView));
    }

    @Test
    void shouldReturnFalseWhenStateIsValid() {
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.VALID);

        assertFalse(underTest.isCertFullyExpired(clusterView));
    }

    @Test
    void shouldReturnFalseWhenDetailsAreBlank() {
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.HOST_CERT_EXPIRING);
        when(clusterView.getCertExpirationDetails()).thenReturn("");

        assertFalse(underTest.isCertFullyExpired(clusterView));
    }

    @Test
    void shouldReturnFalseWhenDetailsAreNull() {
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.HOST_CERT_EXPIRING);
        when(clusterView.getCertExpirationDetails()).thenReturn(null);

        assertFalse(underTest.isCertFullyExpired(clusterView));
    }

    @Test
    void shouldReturnTrueWhenMultipleHostsAndOneHasZeroDays() {
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.HOST_CERT_EXPIRING);
        when(clusterView.getCertExpirationDetails()).thenReturn(
                "[worker0]: Certificate of Cloudera Manager Agent will expire within 0 days. "
                        + "[master0]: Certificate of Cloudera Manager Agent will expire within 0 days.");

        assertTrue(underTest.isCertFullyExpired(clusterView));
    }

    @Test
    void isAnyCertExpiredOnHostsShouldReturnFalseWhenSaltStateSucceeds() throws CloudbreakOrchestratorFailedException {
        Node node = new Node("10.0.0.1", null, null, null, "host1.example.com", null);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(node));
        doNothing().when(saltService).executeSaltState(eq(stackDto), eq(Set.of("host1.example.com")), any());

        assertFalse(underTest.isAnyCertExpiredOnHosts(stackDto));
    }

    @Test
    void isAnyCertExpiredOnHostsShouldReturnTrueWhenSaltStdoutContainsExpiredMessage() throws CloudbreakOrchestratorFailedException {
        Node node = new Node("10.0.0.1", null, null, null, "host1.example.com", null);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(node));
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("host1.example.com", "stderr=Certificate check error: Certificate is expired, comment=Command failed");
        doThrow(new CloudbreakOrchestratorFailedException("Salt state failed", nodesWithErrors))
                .when(saltService).executeSaltState(eq(stackDto), eq(Set.of("host1.example.com")), any());

        assertTrue(underTest.isAnyCertExpiredOnHosts(stackDto));
    }

    @Test
    void isAnyCertExpiredOnHostsShouldReturnFalseWhenSaltFailsWithoutExpiredMessage() throws CloudbreakOrchestratorFailedException {
        Node node = new Node("10.0.0.1", null, null, null, "host1.example.com", null);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(node));
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("host1.example.com", "stderr=Some other error, comment=Command failed");
        doThrow(new CloudbreakOrchestratorFailedException("Salt state failed", nodesWithErrors))
                .when(saltService).executeSaltState(eq(stackDto), eq(Set.of("host1.example.com")), any());

        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.isAnyCertExpiredOnHosts(stackDto));
    }

    @Test
    void validateCertificateFullyExpiredShouldReturnTrueWhenDbSaysFullyExpired() throws CloudbreakOrchestratorFailedException {
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.HOST_CERT_EXPIRING);
        when(clusterView.getCertExpirationDetails()).thenReturn(
                "[master0]: Certificate of Cloudera Manager Agent will expire within 0 days. Critical threshold: 7.");

        assertTrue(underTest.validateCertificateFullyExpired(stackDto));
    }

    @Test
    void validateCertificateFullyExpiredShouldReturnFalseWhenHostsAreHealthy() throws CloudbreakOrchestratorFailedException {
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.VALID);
        when(clusterView.getId()).thenReturn(1L);
        when(runtimeVersionService.getRuntimeVersion(1L)).thenReturn(Optional.of("7.2.18"));
        when(apiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getExtendedHostStatuses(Optional.of("7.2.18"))).thenReturn(extendedHostStatuses);
        when(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.HOST)).thenReturn(false);
        when(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.CERTIFICATE)).thenReturn(false);

        assertFalse(underTest.validateCertificateFullyExpired(stackDto));
    }

    @Test
    void validateCertificateFullyExpiredShouldReturnTrueWhenUnhealthyAndSaltConfirmsExpired() throws CloudbreakOrchestratorFailedException {
        Node node = new Node("10.0.0.1", null, null, null, "host1.example.com", null);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(node));
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.VALID);
        when(clusterView.getId()).thenReturn(1L);
        when(runtimeVersionService.getRuntimeVersion(1L)).thenReturn(Optional.of("7.2.18"));
        when(apiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getExtendedHostStatuses(Optional.of("7.2.18"))).thenReturn(extendedHostStatuses);
        when(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.HOST)).thenReturn(true);
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("host1.example.com", "stderr=Certificate check error: Certificate is expired, comment=Command failed");
        doThrow(new CloudbreakOrchestratorFailedException("Salt state failed", nodesWithErrors))
                .when(saltService).executeSaltState(eq(stackDto), eq(Set.of("host1.example.com")), any());

        assertTrue(underTest.validateCertificateFullyExpired(stackDto));
    }

    @Test
    void validateCertificateFullyExpiredShouldThrowWhenUnhealthyButSaltSaysCertsNotExpired() throws CloudbreakOrchestratorFailedException {
        Node node = new Node("10.0.0.1", null, null, null, "host1.example.com", null);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getAllFunctioningNodes()).thenReturn(Set.of(node));
        when(clusterView.getCertExpirationState()).thenReturn(CertExpirationState.VALID);
        when(clusterView.getId()).thenReturn(1L);
        when(runtimeVersionService.getRuntimeVersion(1L)).thenReturn(Optional.of("7.2.18"));
        when(apiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getExtendedHostStatuses(Optional.of("7.2.18"))).thenReturn(extendedHostStatuses);
        when(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.HOST)).thenReturn(true);
        doNothing().when(saltService).executeSaltState(eq(stackDto), eq(Set.of("host1.example.com")), any());

        assertThrows(SecretRotationException.class, () -> underTest.validateCertificateFullyExpired(stackDto));
    }
}
