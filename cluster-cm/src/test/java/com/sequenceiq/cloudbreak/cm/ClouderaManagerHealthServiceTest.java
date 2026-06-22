package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.SUMMARY;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService.FULL_WITH_EXPLANATION_VIEW;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService.HOST_AGENT_CERTIFICATE_EXPIRY;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService.HOST_SCM_HEALTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiClusterRef;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatusesFactory;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.healthcheck.ClouderaManagerHostBasicHealthCheck;
import com.sequenceiq.cloudbreak.cm.healthcheck.ClouderaManagerHostCertHealthCheck;
import com.sequenceiq.cloudbreak.cm.healthcheck.ClouderaManagerHostServiceConfigStalenessHealthCheck;
import com.sequenceiq.cloudbreak.cm.healthcheck.ClouderaManagerHostServicesHealthCheck;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerHealthServiceTest {

    private static final String CLUSTER_NAME = "cluster";

    @InjectMocks
    private ClouderaManagerHealthService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient client;

    @Mock
    private HostsResourceApi hostsResourceApi;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private ApiServiceList apiServiceList;

    @Mock
    private ExtendedHostStatusesFactory extendedHostStatusesFactory;

    @BeforeEach
    void setUp() throws ApiException {
        ReflectionTestUtils.setField(underTest, "hostHealthChecks", Set.of(
                new ClouderaManagerHostBasicHealthCheck(),
                new ClouderaManagerHostCertHealthCheck(),
                new ClouderaManagerHostServiceConfigStalenessHealthCheck(),
                new ClouderaManagerHostServicesHealthCheck()
        ));
        lenient().when(clouderaManagerApiFactory.getHostsResourceApi(client)).thenReturn(hostsResourceApi);
        lenient().when(clouderaManagerApiFactory.getServicesResourceApi(client)).thenReturn(servicesResourceApi);
        lenient().when(servicesResourceApi.readServices(CLUSTER_NAME, ClouderaManagerConstants.SUMMARY)).thenReturn(apiServiceList);
        lenient().when(extendedHostStatusesFactory.create(any())).thenAnswer(invocation -> new ExtendedHostStatuses(invocation.getArgument(0)));
    }

    @Test
    public void collectsHostHealthIfDuplicated() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD)),
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD)),
                host("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        Optional<String> reason = extendedHostStatuses.getHostsHealth().get(hostName("host1")).iterator().next().getReason();
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("This host is duplicated"));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void collectsHostHealthIfAvailable() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD)),
                host("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING)),
                host("host3")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD)),
                host("host4")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.NOT_AVAILABLE)),
                host("host5")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.HISTORY_NOT_AVAILABLE)),
                host("host6")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.DISABLED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host3")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void collectsExtendedHostHealthIfAvailable() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                host("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.CONCERNING)
                                .explanation("in 30 days")),
                host("host3")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD)
                                .explanation("explanation."))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.BAD)
                                .explanation("in 2 days")),
                host("host4")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.NOT_AVAILABLE)),
                host("host5")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.HISTORY_NOT_AVAILABLE)),
                host("host6")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.DISABLED)),
                host("host7")
                        .maintenanceMode(true)
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertEquals("explanation. Certificate health in Cloudera Manager is BAD", extendedHostStatuses.statusReasonForHost(hostName("host3")));
        assertTrue(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.CERTIFICATE));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host3")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host7")));
    }

    @Test
    public void testCertExpiringEverythingElseGood() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                host("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.CONCERNING)
                                .explanation("in 30 days"))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertTrue(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.CERTIFICATE));
    }

    @MethodSource("configStalenessSource")
    @ParameterizedTest
    public void testConfigStalenessEverythingElseGood(ApiConfigStalenessStatus configStatus, ApiConfigStalenessStatus clientConfigStatus, boolean healthy)
            throws ApiException {
        hostsAre(host("host1").addRoleRefsItem(new ApiRoleRef().serviceName("knox")));
        when(apiServiceList.getItems()).thenReturn(List.of(
                new ApiService().name("knox").configStalenessStatus(configStatus).clientConfigStalenessStatus(clientConfigStatus)
        ));

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertEquals(!healthy, extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.SERVICE_CONFIG_STALENESS));
    }

    public static Stream<Arguments> configStalenessSource() {
        return Stream.of(
                Arguments.of(ApiConfigStalenessStatus.FRESH, ApiConfigStalenessStatus.FRESH, true),
                Arguments.of(ApiConfigStalenessStatus.FRESH, ApiConfigStalenessStatus.STALE, false),
                Arguments.of(ApiConfigStalenessStatus.STALE, ApiConfigStalenessStatus.FRESH, false),
                Arguments.of(ApiConfigStalenessStatus.STALE_REFRESHABLE, ApiConfigStalenessStatus.FRESH, false),
                Arguments.of(ApiConfigStalenessStatus.FRESH, ApiConfigStalenessStatus.STALE_REFRESHABLE, false)
        );
    }

    @Test
    public void testServiceBadHealth() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD)
                                .explanation("explanation"))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("badservice", ApiHealthSummary.BAD)),
                host("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)
                                .explanation("in 30 days"))
                        .addRoleRefsItem(roleRef("badservice2", ApiHealthSummary.BAD))
                        .addRoleRefsItem(roleRef("badservice3", ApiHealthSummary.BAD))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.CERTIFICATE));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertEquals("explanation. The following services are in bad health: badservice.",
                extendedHostStatuses.statusReasonForHost(hostName("host1")));
        assertEquals("The following services are in bad health: badservice2, badservice3.",
                extendedHostStatuses.statusReasonForHost(hostName("host2")));

        extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.11"));

        assertFalse(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.CERTIFICATE));
        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
        assertEquals("explanation.",
                extendedHostStatuses.statusReasonForHost(hostName("host1")));
    }

    @Test
    public void testEverythingGood() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                host("host2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)
                                .explanation("in 30 days"))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.CERTIFICATE));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host2")));
    }

    @Test
    public void testStoppedServiceOnAliveHost() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("stoppedService", ApiHealthSummary.GOOD, ApiRoleState.STOPPED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.statusReasonForHost(hostName("host1")).contains("The following services are stopped: stoppedService."));
    }

    @Test
    public void testStoppingServiceOnAliveHost() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("stoppingService", ApiHealthSummary.GOOD, ApiRoleState.STOPPING))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertTrue(extendedHostStatuses.statusReasonForHost(hostName("host1")).contains("The following services are stopped: stoppingService."));
    }

    @Test
    public void testStoppedServiceOnDeadHostNotFlagged() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD))
                        .addRoleRefsItem(roleRef("stoppedService", ApiHealthSummary.GOOD, ApiRoleState.STOPPED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        assertFalse(extendedHostStatuses.statusReasonForHost(hostName("host1")).contains("stopped"));
    }

    @Test
    public void testStoppedServiceOnMaintenanceModeHostNotFlagged() throws ApiException {
        hostsAre(
                host("host1")
                        .maintenanceMode(true)
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("stoppedService", ApiHealthSummary.GOOD, ApiRoleState.STOPPED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.statusReasonForHost(hostName("host1")).contains("stopped"));
    }

    @Test
    public void testBothBadHealthAndStoppedServices() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("badService", ApiHealthSummary.BAD, ApiRoleState.STARTED))
                        .addRoleRefsItem(roleRef("stoppedService", ApiHealthSummary.GOOD, ApiRoleState.STOPPED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertFalse(extendedHostStatuses.isHostHealthy(hostName("host1")));
        String reason = extendedHostStatuses.statusReasonForHost(hostName("host1"));
        assertTrue(reason.contains("The following services are in bad health: badService."));
        assertTrue(reason.contains("The following services are stopped: stoppedService."));
    }

    @Test
    public void testRunningServicesOnAliveHostStaysHealthy() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("runningService", ApiHealthSummary.GOOD, ApiRoleState.STARTED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.12"));

        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
    }

    @Test
    public void testStoppedServiceNotFlaggedBelowVersionGate() throws ApiException {
        hostsAre(
                host("host1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(roleRef("stoppedService", ApiHealthSummary.GOOD, ApiRoleState.STOPPED))
        );

        ExtendedHostStatuses extendedHostStatuses = underTest.getExtendedHostStatuses(client, Optional.of("7.2.11"));

        assertTrue(extendedHostStatuses.isHostHealthy(hostName("host1")));
    }

    @Test
    public void filtersAppropriateHealthCheckForHost() throws ApiException {
        hostsAre(
                host("host")
                        .addHealthChecksItem(new ApiHealthCheck().name("fake_check").summary(ApiHealthSummary.BAD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name("another").summary(ApiHealthSummary.BAD)),
                host("host_non_cluster")
                        .clusterRef(null)
        );

        assertTrue(underTest.getExtendedHostStatuses(client, Optional.of("7.2.12")).isHostHealthy(hostName("host")));
    }

    private void hostsAre(ApiHost... hosts) throws ApiException {
        Arrays.stream(hosts).forEach(host -> host.addRoleRefsItem(roleRef(ApiHealthSummary.GOOD)));
        ApiHostList list = new ApiHostList().items(Arrays.asList(hosts));
        lenient().when(hostsResourceApi.readHosts(null, null, FULL_VIEW)).thenReturn(list);
        lenient().when(hostsResourceApi.readHosts(null, null, SUMMARY)).thenReturn(list);
        lenient().when(hostsResourceApi.readHosts(null, null, FULL_WITH_EXPLANATION_VIEW)).thenReturn(list);
    }

    private static ApiHost host(String hostName) {
        return new ApiHost().hostname(hostName).clusterRef(clusterRef());
    }

    private static ApiClusterRef clusterRef() {
        return new ApiClusterRef().clusterName(CLUSTER_NAME);
    }

    private ApiRoleRef roleRef(ApiHealthSummary apiHealthSummary) {
        return roleRef("testService", apiHealthSummary);
    }

    private ApiRoleRef roleRef(String serviceName, ApiHealthSummary apiHealthSummary) {
        return new ApiRoleRef()
                .roleName("testRole")
                .serviceName(serviceName)
                .healthSummary(apiHealthSummary);
    }

    private ApiRoleRef roleRef(String serviceName, ApiHealthSummary apiHealthSummary, ApiRoleState roleState) {
        return new ApiRoleRef()
                .roleName("testRole")
                .serviceName(serviceName)
                .healthSummary(apiHealthSummary)
                .roleStatus(roleState);
    }
}