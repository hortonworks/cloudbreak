package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.SUMMARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
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
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

import okhttp3.OkHttpClient;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerClusterStatusServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private final HttpClientConfig clientConfig = new HttpClientConfig("1.2.3.4", null, null, null);

    @Mock
    private ApiClient client;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private RolesResourceApi rolesResourceApi;

    @Mock
    private HostsResourceApi hostsResourceApi;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Mock
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Mock
    private MetricService metricService;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private ClouderaManagerHealthService clouderaManagerHealthService;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private Stack stack;

    private Cluster cluster;

    private ClouderaManagerClusterStatusService subject;

    @BeforeEach
    public void init() throws ClouderaManagerClientInitException, ClusterClientInitException {
        cluster = new Cluster();
        cluster.setName(CLUSTER_NAME);
        stack = new Stack();
        stack.setName(CLUSTER_NAME);
        stack.setCluster(cluster);

        subject = new ClouderaManagerClusterStatusService(stack, clientConfig);
        ReflectionTestUtils.setField(subject, "clouderaManagerApiClientProvider", clouderaManagerApiClientProvider);
        ReflectionTestUtils.setField(subject, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(subject, "cmApiRetryTemplate", retryTemplate);
        ReflectionTestUtils.setField(subject, "syncApiCommandRetriever", syncApiCommandRetriever);
        ReflectionTestUtils.setField(subject, "clouderaManagerCommandsService", clouderaManagerCommandsService);
        ReflectionTestUtils.setField(subject, "connectQuickTimeoutSeconds", 1);
        ReflectionTestUtils.setField(subject, "metricService", metricService);
        ReflectionTestUtils.setField(subject, "clouderaManagerHealthService", clouderaManagerHealthService);
        ReflectionTestUtils.setField(subject, "clouderaManagerPollingServiceProvider", clouderaManagerPollingServiceProvider);
        when(okHttpClient.newBuilder()).thenReturn(new OkHttpClient.Builder());
        when(client.getHttpClient()).thenReturn(okHttpClient);
        when(clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), cluster.getCloudbreakClusterManagerUser(),
                cluster.getPassword(), clientConfig)).thenReturn(client);
        lenient().when(clouderaManagerApiFactory.getClouderaManagerResourceApi(client)).thenReturn(clouderaManagerResourceApi);
        lenient().when(clouderaManagerApiFactory.getServicesResourceApi(client)).thenReturn(servicesResourceApi);
        lenient().when(clouderaManagerApiFactory.getRolesResourceApi(client)).thenReturn(rolesResourceApi);
        lenient().when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        subject.initApiClient();
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
        assertTrue(statusReason.contains("STARTED: service1"), statusReason);
        assertTrue(statusReason.contains("INSTALLED: service2"), statusReason);
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
        assertTrue(statusReason.contains("STARTED: role 1.1, role 2.1"), statusReason);
        assertTrue(statusReason.contains("INSTALLED: role 1.2, role 2.2"), statusReason);
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
    public void testGetActiveCommandsListWhenListNotEmpty() throws Exception {
        //GIVEN
        when(clouderaManagerResourceApi.listActiveCommands("SUMMARY"))
                .thenReturn(new ApiCommandList()
                        .items(List.of(new ApiCommand().name("cmd").id(BigDecimal.ONE).startTime("starttime"))));
        // WHEN
        List<ClusterManagerCommand> actualResult = subject.getActiveCommandsList();
        // THEN
        assertEquals(1, actualResult.size());
        ClusterManagerCommand actualCmd = actualResult.get(0);
        assertEquals(BigDecimal.ONE, actualCmd.getId());
        assertEquals("cmd", actualCmd.getName());
        assertEquals("starttime", actualCmd.getStartTime());
    }

    @Test
    public void testGetActiveCommandsListWhenListIsEmpty() throws Exception {
        //GIVEN
        when(clouderaManagerResourceApi.listActiveCommands("SUMMARY")).thenReturn(new ApiCommandList().items(List.of()));
        // WHEN
        List<ClusterManagerCommand> actualResult = subject.getActiveCommandsList();
        // THEN
        assertEquals(0, actualResult.size());
    }

    @Test
    public void testGetActiveCommandsListWhenListIsNull() throws Exception {
        //GIVEN
        when(clouderaManagerResourceApi.listActiveCommands("SUMMARY")).thenReturn(new ApiCommandList());
        // WHEN
        List<ClusterManagerCommand> actualResult = subject.getActiveCommandsList();
        // THEN
        assertEquals(0, actualResult.size());
    }

    @Test
    public void testGetActiveCommandsListWhenApiCallThrowsException() throws Exception {
        //GIVEN
        when(clouderaManagerResourceApi.listActiveCommands("SUMMARY")).thenThrow(new ApiException());
        // WHEN

        assertThrows(ClouderaManagerOperationFailedException.class, () -> subject.getActiveCommandsList());
        // THEN ClouderaManagerOperationFailedException is thrown
    }

    @Test
    void testIsServiceRunningByType() throws ApiException {
        ApiServiceList apiServiceList = mock(ApiServiceList.class);
        ApiService apiService = mock(ApiService.class);
        when(apiService.getType()).thenReturn("serviceType");
        when(apiService.getServiceState()).thenReturn(ApiServiceState.STARTED);
        when(apiServiceList.getItems()).thenReturn(List.of(apiService));
        when(servicesResourceApi.readServices(any(), any())).thenReturn(apiServiceList);

        boolean result = subject.isServiceRunningByType("clusterName", "serviceType");

        assertTrue(result);
        verify(servicesResourceApi, times(1)).readServices("clusterName", "summary");
    }

    @Test
    void testIsServiceRunningByTypeServiceInBadState() throws ApiException {
        ApiServiceList apiServiceList = mock(ApiServiceList.class);
        ApiService apiService = mock(ApiService.class);
        when(apiService.getType()).thenReturn("serviceType");
        when(apiService.getServiceState()).thenReturn(ApiServiceState.STOPPED);
        when(apiServiceList.getItems()).thenReturn(List.of(apiService));
        when(servicesResourceApi.readServices(any(), any())).thenReturn(apiServiceList);

        boolean result = subject.isServiceRunningByType("clusterName", "serviceType");

        assertFalse(result);
        verify(servicesResourceApi, times(1)).readServices("clusterName", "summary");
    }

    @Test
    void testIsServiceRunningByTypeEmptyServiceList() throws ApiException {
        ApiServiceList apiServiceList = mock(ApiServiceList.class);
        when(apiServiceList.getItems()).thenReturn(Collections.emptyList());
        when(servicesResourceApi.readServices(any(), any())).thenReturn(apiServiceList);

        boolean result = subject.isServiceRunningByType("clusterName", "serviceType");

        assertFalse(result);
        verify(servicesResourceApi, times(1)).readServices("clusterName", "summary");
    }

    @Test
    void testIsServiceRunningByTypeThrowApiException() throws ApiException {
        doThrow(ApiException.class).when(servicesResourceApi).readServices(any(), any());

        boolean result = subject.isServiceRunningByType("clusterName", "serviceType");

        assertFalse(result);
        verify(servicesResourceApi, times(1)).readServices("clusterName", "summary");
    }

    @Test
    void testClusterManagerRunningQuickCheckShouldCallMetricServiceIfCallFails() throws ApiException {
        doThrow(new ApiException(123, new HashMap<>(), "{\"code\":\"errorCode\"}")).when(clouderaManagerResourceApi).getVersion();

        boolean result = subject.isClusterManagerRunningQuickCheck();

        assertFalse(result);
        verify(clouderaManagerApiFactory, times(1)).getClouderaManagerResourceApi(any());
        verify(clouderaManagerResourceApi, times(1)).getVersion();
        verify(metricService, times(1)).incrementMetricCounter(eq("stack_sync_cm_unreachable"), any(String[].class));
    }

    @Test
    void testWaitForHostHealthyServicesDelegatesToPollingProvider() throws ClusterClientInitException {
        Set<InstanceMetadataView> hosts = Set.of(mock(InstanceMetadataView.class));
        Optional<String> runtimeVersion = Optional.of("7.2.17");
        ExtendedPollingResult expectedResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();

        when(clouderaManagerPollingServiceProvider.startPollingCmSpecificHostsServicesHealthy(eq(stack), eq(client),
                eq(clouderaManagerHealthService), eq(runtimeVersion), eq(hosts))).thenReturn(expectedResult);

        ExtendedPollingResult result = subject.waitForHostHealthyServices(hosts, runtimeVersion);

        assertEquals(expectedResult, result);
        verify(clouderaManagerPollingServiceProvider).startPollingCmSpecificHostsServicesHealthy(eq(stack), eq(client),
                eq(clouderaManagerHealthService), eq(runtimeVersion), eq(hosts));
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
        lenient().when(hostsResourceApi.readHosts(null, null, FULL_VIEW)).thenReturn(list);
        lenient().when(hostsResourceApi.readHosts(null, null, SUMMARY)).thenReturn(list);
    }

    private void servicesAre(ApiService... services) throws ApiException {
        ApiServiceList serviceList = new ApiServiceList().items(Arrays.asList(services));
        when(servicesResourceApi.readServices(CLUSTER_NAME, FULL_VIEW)).thenReturn(serviceList);
    }

    private void rolesAre(String service, ApiRole... roles) throws ApiException {
        ApiRoleList roleList = new ApiRoleList().items(Arrays.asList(roles));
        when(rolesResourceApi.readRoles(eq(CLUSTER_NAME), eq(service), anyString(), eq(FULL_VIEW))).thenReturn(roleList);
    }

    private void cmIsNotReachable() throws ApiException {
        try {
            when(retryTemplate.execute(any())).thenThrow(new ApiException("CM is not reachable"));
        } catch (Throwable t) {
            throw new ApiException(t);
        }
    }

    private void cmIsReachable() throws ApiException {
        lenient().when(clouderaManagerResourceApi.getVersion()).thenReturn(new ApiVersionInfo().version("1.2.3"));
    }

}
