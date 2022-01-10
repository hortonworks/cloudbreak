package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_WITH_EXPLANATION_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.HOST_AGENT_CERTIFICATE_EXPIRY;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.HOST_SCM_HEALTH;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
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
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
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
    public void reportsClusterUnknownWhenARoleStateIsNull() throws ApiException {
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
                new ApiRole().name("role 2.1").roleState(null),
                new ApiRole().name("role 2.2").roleState(ApiRoleState.STARTED)
        );

        assertEquals(ClusterStatus.UNKNOWN, subject.getStatus(true).getClusterStatus());
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

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.12"));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host3")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void getDecommissionedHostsFromCM() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host1").maintenanceMode(true),
                new ApiHost().hostname("host2").maintenanceMode(false)
        );

        List<String> hosts = subject.getDecommissionedHostsFromCM();
        assertEquals(1, hosts.size());
        assertEquals("host1", hosts.get(0));
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
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD).explanation("explanation."))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.BAD).explanation("in 2 days")),
                new ApiHost().hostname("host4").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.NOT_AVAILABLE)),
                new ApiHost().hostname("host5").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.HISTORY_NOT_AVAILABLE)),
                new ApiHost().hostname("host6").addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.DISABLED))
        );

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.12"));
        assertEquals("explanation. Cert health on CM: BAD", extendedHostStatuses.statusReasonForHost(hostName("host3")));
        assertTrue(extendedHostStatuses.isAnyCertExpiring());
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host3")));
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

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.12"));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertTrue(extendedHostStatuses.isAnyCertExpiring());
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

        assertFalse(extendedHostStatuses.isAnyCertExpiring());
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertEquals("explanation. The following services are in bad health: badservice.",
                extendedHostStatuses.statusReasonForHost(hostName("host1")));
        assertEquals("The following services are in bad health: badservice2, badservice3.",
                extendedHostStatuses.statusReasonForHost(hostName("host2")));

        extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.11"));

        assertFalse(extendedHostStatuses.isAnyCertExpiring());
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertEquals("explanation.",
                extendedHostStatuses.statusReasonForHost(hostName("host1")));
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

        ExtendedHostStatuses extendedHostStatuses = subject.getExtendedHostStatuses(Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isAnyCertExpiring());
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void filtersAppropriateHealthCheckForHost() throws ApiException {
        hostsAre(
                new ApiHost().hostname("host")
                        .addHealthChecksItem(new ApiHealthCheck().name("fake_check").summary(ApiHealthSummary.BAD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name("another").summary(ApiHealthSummary.BAD))
        );

        assertTrue(subject.getExtendedHostStatuses(Optional.of("7.2.12")).isHostHealthy(hostName("host")));
    }

    @Test
    public void hostWithoutHealthCheckIsIgnored() throws ApiException {
        hostsAre(new ApiHost().hostname("hostY"));

        assertFalse(subject.getExtendedHostStatuses(Optional.of("7.2.12")).getHostsHealth().containsKey(hostName("hostY")));
    }

    @Test
    public void hostWithoutAppropriateHealthCheckIsIgnored() throws ApiException {
        hostsAre(new ApiHost().hostname("hostY").addHealthChecksItem(new ApiHealthCheck().name("fake_check").summary(ApiHealthSummary.BAD)));

        assertFalse(subject.getExtendedHostStatuses(Optional.of("7.2.12")).getHostsHealth().containsKey(hostName("hostY")));
    }

    @Test
    public void testGetActiveCommandsListWhenListNotEmpty() throws Exception {
        //GIVEN
        when(cmApi.listActiveCommands("SUMMARY"))
                .thenReturn(new ApiCommandList()
                        .items(List.of(new ApiCommand().name("cmd").id(BigDecimal.ONE).startTime("starttime"))));
        // WHEN
        List<String> actualResult = subject.getActiveCommandsList();
        // THEN
        assertEquals(1, actualResult.size());
        String actualCmd = actualResult.get(0);
        assertEquals("ApiCommand[id: 1, name: cmd, starttime: starttime]", actualCmd);
    }

    @Test
    public void testGetActiveCommandsListWhenListIsEmpty() throws Exception {
        //GIVEN
        when(cmApi.listActiveCommands("SUMMARY")).thenReturn(new ApiCommandList().items(List.of()));
        // WHEN
        List<String> actualResult = subject.getActiveCommandsList();
        // THEN
        assertEquals(0, actualResult.size());
    }

    @Test
    public void testGetActiveCommandsListWhenListIsNull() throws Exception {
        //GIVEN
        when(cmApi.listActiveCommands("SUMMARY")).thenReturn(new ApiCommandList());
        // WHEN
        List<String> actualResult = subject.getActiveCommandsList();
        // THEN
        assertEquals(0, actualResult.size());
    }

    @Test(expected = ClouderaManagerOperationFailedException.class)
    public void testGetActiveCommandsListWhenApiCallThrowsException() throws Exception {
        //GIVEN
        when(cmApi.listActiveCommands("SUMMARY")).thenThrow(new ApiException());
        // WHEN
        subject.getActiveCommandsList();
        // THEN ClouderaManagerOperationFailedException is thrown
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
        when(hostsApi.readHosts(null, null, SUMMARY)).thenReturn(list);
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
