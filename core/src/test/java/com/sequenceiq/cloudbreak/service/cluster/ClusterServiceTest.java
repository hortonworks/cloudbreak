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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
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
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
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
    private StackService stackService;

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

    @Captor
    private ArgumentCaptor<Iterable<InstanceMetaData>> payloadArgumentCaptor;

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
                {"CM returns HEALTHY", HEALTHY, InstanceStatus.SERVICES_HEALTHY, ""},
                {"CM returns UNHEALTHY", UNHEALTHY, InstanceStatus.SERVICES_UNHEALTHY, STATUS_REASON_SERVER},
                {"CM returns no data for the host", null, InstanceStatus.SERVICES_RUNNING, STATUS_REASON_ORIGINAL},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("updateClusterMetadataScenarios")
    public void testUpdateClusterMetadataWhenCmReachable(
            String name, HealthCheckResult healthCheckResult, InstanceStatus expectedInstanceStatus, String expectedStatusReason)
            throws TransactionService.TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).thenAnswer(ans -> ((Supplier) ans.getArgument(0)).get());
        when(runtimeVersionService.getRuntimeVersion(anyLong())).thenReturn(Optional.of("7.2.11"));
        Stack stack = setupStack(STACK_ID);
        setupClusterApi(stack, healthCheckResult, expectedStatusReason);
        setupInstanceMetadata(stack);

        underTest.updateClusterMetadata(STACK_ID);

        verify(instanceMetaDataService).saveAll(payloadArgumentCaptor.capture());
        payloadArgumentCaptor.getValue().forEach(imd -> {
            if (FQDN1.equals(imd.getDiscoveryFQDN())) {
                assertEquals(expectedInstanceStatus, imd.getInstanceStatus());
                assertEquals(expectedStatusReason, imd.getStatusReason());
            }
        });
    }

    @Test
    void testIsRangerRazEnabledOnClusterReturnsTrueWhenCmIsRunning() {
        Stack stack = setupStack(STACK_ID);
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
        Stack stack = setupStack(STACK_ID);
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
        Stack stack = setupStack(STACK_ID);
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

    private Stack setupStack(long stackId) {
        Stack stack = new Stack();
        stack.setId(stackId);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        cluster.setName(CLUSTER_NAME);
        stack.setCluster(cluster);
        lenient().when(stackService.getById(anyLong())).thenReturn(stack);
        return stack;
    }

    private void setupInstanceMetadata(Stack stack) {
        InstanceMetaData instanceMetadata = new InstanceMetaData();
        instanceMetadata.setDiscoveryFQDN(FQDN1);
        instanceMetadata.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        instanceMetadata.setStatusReason(STATUS_REASON_ORIGINAL);
        when(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(stack.getId())).thenReturn(Set.of(instanceMetadata));
    }

    private void setupClusterApi(Stack stack, HealthCheckResult healthCheckResult, String statusReason) {
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
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
    }
}