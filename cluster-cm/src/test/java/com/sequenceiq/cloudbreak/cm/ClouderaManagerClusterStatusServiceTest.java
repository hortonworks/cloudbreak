package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_WITH_EXPLANATION_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.HOST_AGENT_CERTIFICATE_EXPIRY;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.HOST_SCM_HEALTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.support.RetryTemplate;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState.ClusterManagerStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class ClouderaManagerClusterStatusServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private final HttpClientConfig clientConfig = new HttpClientConfig("1.2.3.4", null, null, null);

    @Mock
    private ApiClient client;

    @Mock
    private ClouderaManagerApiFactory clientFactory;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerResourceApi cmApi;

    @Mock
    private ServicesResourceApi servicesApi;

    @Mock
    private RolesResourceApi rolesApi;

    @Mock
    private HostsResourceApi hostsApi;

    @Mock
    private RetryTemplate retryTemplate;

    @InjectMocks
    private ClouderaManagerClusterStatusService subject;

    @Before
    public void init() throws ClouderaManagerClientInitException {
        Cluster cluster = new Cluster();
        cluster.setName(CLUSTER_NAME);
        Stack stack = new Stack();
        stack.setName(CLUSTER_NAME);
        stack.setCluster(cluster);

        subject = new ClouderaManagerClusterStatusService(stack, clientConfig);

        MockitoAnnotations.initMocks(this);

        when(clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), cluster.getCloudbreakAmbariUser(),
                cluster.getPassword(), clientConfig)).thenReturn(client);
        when(clientFactory.getClouderaManagerResourceApi(client)).thenReturn(cmApi);
        when(clientFactory.getServicesResourceApi(client)).thenReturn(servicesApi);
        when(clientFactory.getRolesResourceApi(client)).thenReturn(rolesApi);
        when(clientFactory.getHostsResourceApi(client)).thenReturn(hostsApi);
    }

    @Test
    public void detectsStoppedClouderaManager() throws ApiException {
        cmIsNotReachable();

        assertEquals(ClusterStatus.CLUSTERMANAGER_NOT_RUNNING, subject.getStatus(true).getClusterStatus());
    }

    @Test
    public void reportsClouderaManagerRunningWhenClusterNotPresent() throws ApiException {
        cmIsReachable();

        assertEquals(ClusterStatus.CLUSTERMANAGER_RUNNING, subject.getStatus(false).getClusterStatus());
    }

    @Test
    public void reportsClusterStatusPendingWhenAnyServiceIsStarting() throws ApiException {
        cmIsReachable();
        servicesAre(
            new ApiService().name("service1").serviceState(ApiServiceState.STARTING),
            new ApiService().name("service2").serviceState(ApiServiceState.STARTED)
        );

        assertEquals(ClusterStatus.PENDING, subject.getStatus(true).getClusterStatus());
    }

    @Test
    public void reportsClusterStoppedWhenAllServicesStopped() throws ApiException {
        cmIsReachable();
        servicesAre(
                new ApiService().name("service1").serviceState(ApiServiceState.STOPPED),
                new ApiService().name("service2").serviceState(ApiServiceState.STOPPED)
        );

        assertEquals(ClusterStatus.INSTALLED, subject.getStatus(true).getClusterStatus());
    }

    @Test
    public void reportsClusterStartedWhenAllRolesAreStarted() throws ApiException {
        cmIsReachable();
        servicesAre(
                new ApiService().name("service1").serviceState(ApiServiceState.STARTED),
                new ApiService().name("service2").serviceState(ApiServiceState.STARTED)
        );
        rolesAre("service1",
                new ApiRole().name("role 1.1").roleState(ApiRoleState.STARTED),
                new ApiRole().name("role 1.2").roleState(ApiRoleState.STARTED)
        );
        rolesAre("service2",
                new ApiRole().name("role 2.1").roleState(ApiRoleState.STARTED),
                new ApiRole().name("role 2.2").roleState(ApiRoleState.STARTED)
        );

        assertEquals(ClusterStatus.STARTED, subject.getStatus(true).getClusterStatus());
    }

    @Test
    public void ignoresServicesNA() throws ApiException {
        cmIsReachable();
        servicesAre(
                new ApiService().name("service1").serviceState(ApiServiceState.STOPPED),
                new ApiService().name("service2").serviceState(ApiServiceState.NA),
                new ApiService().name("service3").serviceState(ApiServiceState.STOPPED)
        );

        assertEquals(ClusterStatus.INSTALLED, subject.getStatus(true).getClusterStatus());
    }

    @Test
    public void reportsAmbiguousServiceStatus() throws ApiException {
        cmIsReachable();
        servicesAre(
                new ApiService().name("service1").serviceState(ApiServiceState.STARTED),
                new ApiService().name("service2").serviceState(ApiServiceState.STOPPED)
        );

        ClusterStatusResult statusResult = subject.getStatus(true);
        assertEquals(ClusterStatus.AMBIGUOUS, statusResult.getClusterStatus());
        String statusReason = statusResult.getStatusReason();
        assertTrue(statusReason, statusReason.contains("STARTED: service1"));
        assertTrue(statusReason, statusReason.contains("INSTALLED: service2"));
    }

    @Test
    public void reportsAmbiguousRoleStatus() throws ApiException {
        cmIsReachable();
        servicesAre(
                new ApiService().name("service1").serviceState(ApiServiceState.STARTED),
                new ApiService().name("service2").serviceState(ApiServiceState.STARTED)
        );
        rolesAre("service1",
                new ApiRole().name("role 1.1").roleState(ApiRoleState.STARTED),
                new ApiRole().name("role 1.2").roleState(ApiRoleState.STOPPED)
        );
        rolesAre("service2",
                new ApiRole().name("role 2.1").roleState(ApiRoleState.STARTED),
                new ApiRole().name("role 2.2").roleState(ApiRoleState.STOPPED)
        );

        ClusterStatusResult statusResult = subject.getStatus(true);
        assertEquals(ClusterStatus.AMBIGUOUS, statusResult.getClusterStatus());
        String statusReason = statusResult.getStatusReason();
        assertTrue(statusReason, statusReason.contains("STARTED: role 1.1, role 2.1"));
        assertTrue(statusReason, statusReason.contains("INSTALLED: role 1.2, role 2.2"));
    }

    @Test
    public void collectsHostHealthIfAvailable() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING)),
                new ApiHost().hostname("host3").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD)),
                new ApiHost().hostname("host4").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.NOT_AVAILABLE)),
                new ApiHost().hostname("host5").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.HISTORY_NOT_AVAILABLE)),
                new ApiHost().hostname("host6").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.DISABLED))
        );

        Map<HostName, ClusterManagerState.ClusterManagerStatus> expected = ImmutableMap.of(
                hostName("host1"), ClusterManagerState.ClusterManagerStatus.HEALTHY,
                hostName("host2"), ClusterManagerState.ClusterManagerStatus.HEALTHY,
                hostName("host3"), ClusterManagerState.ClusterManagerStatus.UNHEALTHY
        );
        Map<HostName, ClusterManagerState.ClusterManagerStatus> actual = new TreeMap<>(subject.getHostStatuses());
        assertEquals(expected, actual);
    }

    @Test
    public void collectsExtendedHostHealthIfAvailable() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.CONCERNING)
                                .explanation("in 30 days")),
                new ApiHost().hostname("host3")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD).explanation("explanation"))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.BAD).explanation("in 2 days")),
                new ApiHost().hostname("host4").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.NOT_AVAILABLE)),
                new ApiHost().hostname("host5").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.HISTORY_NOT_AVAILABLE)),
                new ApiHost().hostname("host6").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.DISABLED))
        );

        Map<HostName, ClusterManagerState.ClusterManagerStatus> expected = ImmutableMap.of(
                hostName("host1"), ClusterManagerState.ClusterManagerStatus.HEALTHY,
                hostName("host2"), ClusterManagerState.ClusterManagerStatus.HEALTHY,
                hostName("host3"), ClusterManagerState.ClusterManagerStatus.UNHEALTHY
        );

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.11"));
        Map<HostName, ClusterManagerState> extendedStateMap = extendedHostStatuses.getHostHealth();
        Map<HostName, ClusterManagerState.ClusterManagerStatus> actual = new TreeMap<>(extendedStateMap.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), e.getValue().getClusterManagerStatus()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
        assertEquals(expected, actual);
        assertEquals("HOST_SCM_HEALTH: BAD. Reason: explanation. ", extendedStateMap.get(hostName("host3")).getStatusReason());
        assertTrue(extendedHostStatuses.isHostCertExpiring());
    }

    @Test
    public void testCertExpiringEverythingElseGood() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.CONCERNING)
                                .explanation("in 30 days"))
        );

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.11"));
        Map<HostName, ClusterManagerState> extendedStateMap = extendedHostStatuses.getHostHealth();

        Map<HostName, ClusterManagerState> expected = Map.of(hostName("host1"), new ClusterManagerState(ClusterManagerStatus.HEALTHY, null),
                hostName("host2"), new ClusterManagerState(ClusterManagerStatus.HEALTHY, null));

        assertEquals(expected.keySet(), extendedStateMap.keySet());
        extendedStateMap.values().forEach(state -> assertEquals(ClusterManagerStatus.HEALTHY, state.getClusterManagerStatus()));
        assertTrue(extendedHostStatuses.isHostCertExpiring());
    }

    @Test
    public void testServiceBadHealth() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD)
                            .explanation("explanation"))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("badservice", ApiHealthSummary.BAD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)
                                .explanation("in 30 days"))
                        .addRoleRefsItem(roleRef("badservice2", ApiHealthSummary.BAD))
                        .addRoleRefsItem(roleRef("badservice3", ApiHealthSummary.BAD))
        );

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.12"));
        Map<HostName, ClusterManagerState> extendedStateMap = extendedHostStatuses.getHostHealth();

        Map<HostName, ClusterManagerState> expected = Map.of(
                hostName("host1"), new ClusterManagerState(ClusterManagerStatus.UNHEALTHY,
                        "HOST_SCM_HEALTH: BAD. Reason: explanation. The following service are in bad health: badservice"),
                hostName("host2"), new ClusterManagerState(ClusterManagerStatus.UNHEALTHY,
                        "The following service are in bad health: badservice2,badservice3"));

        assertEquals(expected.keySet(), extendedStateMap.keySet());
        extendedStateMap.values().forEach(state -> assertEquals(ClusterManagerStatus.UNHEALTHY, state.getClusterManagerStatus()));
        assertFalse(extendedHostStatuses.isHostCertExpiring());

        extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.11"));
        extendedStateMap = extendedHostStatuses.getHostHealth();

        expected = Map.of(
                hostName("host1"), new ClusterManagerState(ClusterManagerStatus.UNHEALTHY,
                        "HOST_SCM_HEALTH: BAD. Reason: explanation"),
                hostName("host2"), new ClusterManagerState(ClusterManagerStatus.HEALTHY, null));
        assertEquals(expected.keySet(), extendedStateMap.keySet());
        assertEquals(ClusterManagerStatus.UNHEALTHY, expected.get(hostName("host1")).getClusterManagerStatus());
        assertEquals(ClusterManagerStatus.HEALTHY, expected.get(hostName("host2")).getClusterManagerStatus());
        assertFalse(extendedHostStatuses.isHostCertExpiring());
    }

    @Test
    public void testEverythingGood() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)
                                .explanation("in 30 days"))
        );

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.11"));
        Map<HostName, ClusterManagerState> extendedStateMap = extendedHostStatuses.getHostHealth();

        Map<HostName, ClusterManagerState> expected = Map.of(hostName("host1"), new ClusterManagerState(ClusterManagerStatus.HEALTHY, null),
                hostName("host2"), new ClusterManagerState(ClusterManagerStatus.HEALTHY, null));

        assertEquals(expected.keySet(), extendedStateMap.keySet());
        extendedStateMap.values().forEach(state -> assertEquals(ClusterManagerStatus.HEALTHY, state.getClusterManagerStatus()));
        assertFalse(extendedHostStatuses.isHostCertExpiring());
    }

    @Test
    public void filtersAppropriateHealthCheckForHost() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host")
                        .addHealthChecksItem(new ApiHealthCheck().name("fake_check").summary(ApiHealthSummary.BAD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name("another").summary(ApiHealthSummary.BAD))
        );

        Map<HostName, ClusterManagerStatus> expected = Collections.singletonMap(hostName("host"), ClusterManagerStatus.HEALTHY);
        assertEquals(expected, subject.getHostStatuses());
    }

    @Test
    public void ignoresHostsWithoutHealthChecks() throws ApiException {
        hostsAre(new ApiHost().hostname("hostY"));

        assertEquals(Collections.emptyMap(), subject.getHostStatuses());
    }

    @Test
    public void ignoresHostsWithoutAppropriateHealthCheck() throws ApiException {
        hostsAre(new ApiHost().hostname("hostY").addHealthChecksItem(new ApiHealthCheck().name("fake_check").summary(ApiHealthSummary.GOOD)));

        assertEquals(Collections.emptyMap(), subject.getHostStatuses());
    }

    private ApiRoleRef roleRef(String serviceName, ApiHealthSummary apiHealthSummary) {
        return new ApiRoleRef()
                .roleName("testRole")
                .serviceName(serviceName)
                .healthSummary(apiHealthSummary);
    }

    private ApiRoleRef roleRef(ApiHealthSummary apiHealthSummary) {
        return roleRef("testService", apiHealthSummary);
    }

    private void hostsAre(ApiHost... hosts) throws ApiException {
        Arrays.stream(hosts).forEach(host -> host.addRoleRefsItem(roleRef(ApiHealthSummary.GOOD)));
        ApiHostList list = new ApiHostList().items(Arrays.asList(hosts));
        when(hostsApi.readHosts(null, null, FULL_VIEW)).thenReturn(list);
        when(hostsApi.readHosts(null, null, FULL_WITH_EXPLANATION_VIEW)).thenReturn(list);
    }

    private void servicesAre(ApiService... services) throws ApiException {
        ApiServiceList serviceList = new ApiServiceList().items(Arrays.asList(services));
        when(servicesApi.readServices(CLUSTER_NAME, FULL_VIEW)).thenReturn(serviceList);
    }

    private void rolesAre(String service, ApiRole... roles) throws ApiException {
        ApiRoleList roleList = new ApiRoleList().items(Arrays.asList(roles));
        when(rolesApi.readRoles(eq(CLUSTER_NAME), eq(service), anyString(), eq(FULL_VIEW))).thenReturn(roleList);
    }

    private void cmIsNotReachable() throws ApiException {
        try {
            when(retryTemplate.execute(any(), any(), any())).thenThrow(new ApiException("CM is not reachable"));
        } catch (Throwable t) {
            throw new ApiException(t);
        }
    }

    private void cmIsReachable() throws ApiException {
        when(cmApi.getVersion()).thenReturn(new ApiVersionInfo().version("1.2.3"));
    }

}
