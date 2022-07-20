package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.HEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.UNHEALTHY;
import static com.sequenceiq.common.api.type.CertExpirationState.HOST_CERT_EXPIRING;
import static com.sequenceiq.common.api.type.CertExpirationState.VALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.CertExpirationState;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    private static final long STACK_ID = 1L;

    private static final String STATUS_REASON_SERVER = "statusReasonFromServer";

    private static final String STATUS_REASON_ORIGINAL = "statusReasonOriginal";

    private static final String FQDN1 = "hostname1";

    private static final String CLUSTER_NAME = "test-cluster-name";

    private static final String RANGER_RAZ = "RANGER_RAZ";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterRepository repository;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @InjectMocks
    private ClusterService underTest;

    static Object[][] updateClusterCertExpirationStateScenarios() {
        return new Object[][]{
                {"Change from valid to expiring", VALID, Boolean.TRUE, Boolean.TRUE, HOST_CERT_EXPIRING},
                {"Change from expiring to valid", HOST_CERT_EXPIRING, Boolean.FALSE, Boolean.TRUE, VALID},
                {"No change when valid", VALID, Boolean.FALSE, Boolean.FALSE, null},
                {"No change when expiring", HOST_CERT_EXPIRING, Boolean.TRUE, Boolean.FALSE, null}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("updateClusterCertExpirationStateScenarios")
    public void testUpdateClusterCertExpirationState(String name, CertExpirationState current, Boolean hostCertificateExpiring, Boolean stateChanged,
            CertExpirationState newState) {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setCertExpirationState(current);

        underTest.updateClusterCertExpirationState(cluster, hostCertificateExpiring);

        if (stateChanged) {
            verify(repository, times(1)).updateCertExpirationState(cluster.getId(), newState);
        } else {
            verifyNoInteractions(repository);
        }
    }

    static Object[][] updateClusterMetadataScenarios() {
        return new Object[][]{
                {"CM returns HEALTHY", HEALTHY, InstanceStatus.SERVICES_HEALTHY, "", 1},
                {"CM returns UNHEALTHY", UNHEALTHY, InstanceStatus.SERVICES_UNHEALTHY, STATUS_REASON_SERVER, 1},
                {"CM returns no data for the host", null, InstanceStatus.SERVICES_RUNNING, STATUS_REASON_ORIGINAL, 0},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("updateClusterMetadataScenarios")
    public void testUpdateClusterMetadataWhenCmReachable(
            String name, HealthCheckResult healthCheckResult, InstanceStatus expectedInstanceStatus, String expectedStatusReason, int expectedCount) {
        when(runtimeVersionService.getRuntimeVersion(anyLong())).thenReturn(Optional.of("7.2.11"));
        StackDto stack = setupStack(STACK_ID);
        setupClusterApi(stack, healthCheckResult, expectedStatusReason);
        setupInstanceMetadata(stack);
        ArgumentCaptor<InstanceMetadataView> captor = ArgumentCaptor.forClass(InstanceMetadataView.class);

        underTest.updateClusterMetadata(STACK_ID);

        verify(instanceMetaDataService, times(expectedCount)).updateInstanceStatus(captor.capture(), eq(expectedInstanceStatus), eq(expectedStatusReason));
        if (expectedCount > 0) {
            Assertions.assertEquals(FQDN1, captor.getValue().getDiscoveryFQDN());
        }
    }

    @Test
    void testIsRangerRazEnabledOnClusterReturnsTrueWhenCmIsRunning() {
        StackDto stack = setupStack(STACK_ID);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterStatusService clusterStatusService = mock(ClusterStatusService.class);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterStatusService.isClusterManagerRunning()).thenReturn(true);
        when(clusterModificationService.isServicePresent(anyString(), eq(RANGER_RAZ))).thenReturn(true);

        assertTrue(underTest.isRangerRazEnabledOnCluster(stack));
    }

    @Test
    void testIsRangerRazEnabledOnClusterThrowsExceptionIfCmIsNotRunning() {
        StackDto stack = setupStack(STACK_ID);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterStatusService clusterStatusService = mock(ClusterStatusService.class);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.isClusterManagerRunning()).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.isRangerRazEnabledOnCluster(stack));
        assertEquals(String.format("Cloudera Manager is not running for cluster: %s", CLUSTER_NAME), exception.getMessage());
    }

    @Test
    void testIsRangerRazEnabledOnClusterReturnsFalseIfCmIsRunning() {
        StackDto stack = setupStack(STACK_ID);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterStatusService clusterStatusService = mock(ClusterStatusService.class);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterStatusService.isClusterManagerRunning()).thenReturn(true);
        when(clusterModificationService.isServicePresent(anyString(), eq(RANGER_RAZ))).thenReturn(false);

        assertFalse(underTest.isRangerRazEnabledOnCluster(stack));
    }

    private StackDto setupStack(long stackId) {
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getId()).thenReturn(stackId);
        lenient().when(stackDto.getStatus()).thenReturn(Status.AVAILABLE);
        Stack stack = new Stack();
        stack.setId(stackId);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        cluster.setName(CLUSTER_NAME);
        lenient().when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        return stackDto;
    }

    private void setupInstanceMetadata(StackDto stack) {
        InstanceMetaData instanceMetadata = new InstanceMetaData();
        instanceMetadata.setDiscoveryFQDN(FQDN1);
        instanceMetadata.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        instanceMetadata.setStatusReason(STATUS_REASON_ORIGINAL);
        when(stack.getNotTerminatedInstanceMetaData()).thenReturn(List.of(instanceMetadata));
    }

    private void setupClusterApi(StackDto stack, HealthCheckResult healthCheckResult, String statusReason) {
        ClusterApi connector = mock(ClusterApi.class);
        ClusterStatusService clusterStatusService = mock(ClusterStatusService.class);
        when(clusterStatusService.isClusterManagerRunning()).thenReturn(true);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);

        Map<HostName, Set<HealthCheck>> clusterManagerStateMap = new HashMap<>();
        if (healthCheckResult != null) {
            clusterManagerStateMap.put(HostName.hostName(FQDN1),
                    Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, healthCheckResult, Optional.ofNullable(statusReason))));
        }
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(clusterManagerStateMap);
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(connector);
    }
}