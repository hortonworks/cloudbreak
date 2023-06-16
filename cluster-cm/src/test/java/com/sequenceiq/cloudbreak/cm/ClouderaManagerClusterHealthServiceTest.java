package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommissionState;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.status.DetailedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

class ClouderaManagerClusterHealthServiceTest {

    private static final String RESOURCEMANAGER = YarnRoles.RESOURCEMANAGER;

    private static final String NODEMANAGER = YarnRoles.NODEMANAGER;

    private static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    private static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    private static final String FULL_WITH_HEALTH_CHECK_EXPLANATION = "FULL_WITH_HEALTH_CHECK_EXPLANATION";

    private static final String NODE_MANAGER_CONNECTIVITY = "NODE_MANAGER_CONNECTIVITY";

    private static final String NODEMANAGER_LOG_DIRECTORIES_FREE_SPACE = "NODEMANAGER_LOG_DIRECTORIES_FREE_SPACE";

    private static final String RESOURCE_MANAGER_FILE_DESCRIPTOR = "RESOURCE_MANAGER_FILE_DESCRIPTOR";

    private static final String RUNTIME = "7.2.16";

    private static final String STACK_NAME = "test-cluster";

    @Mock
    private ApiClient apiClient;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private HostsResourceApi hostsResourceApi;

    @Mock
    private RolesResourceApi rolesResourceApi;

    @InjectMocks
    private ClouderaManagerClusterHealthService underTest;

    @BeforeEach
    void setUp() {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        Cluster cluster = new Cluster();
        cluster.setName(STACK_NAME);
        stack.setCluster(cluster);

        HttpClientConfig clientConfig = new HttpClientConfig("1.2.3.4", null, null, null);

        underTest = new ClouderaManagerClusterHealthService(stack, clientConfig);

        MockitoAnnotations.openMocks(this);

        when(clouderaManagerApiFactory.getHostsResourceApi(apiClient)).thenReturn(hostsResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(apiClient)).thenReturn(rolesResourceApi);
    }

    @Test
    void testAllGoodHealthChecks() throws ApiException {

        mockHosts(
                new ApiHost().hostname("host-1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host-2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
        );

        DetailedHostStatuses result = underTest.getDetailedHostStatuses(Optional.of(RUNTIME));

        assertThat(result.isHostHealthy(hostName("host-1"))).isTrue();
        assertThat(result.isHostHealthy(hostName("host-2"))).isTrue();
    }

    @Test
    void testUnrecognisedHealthCheckItemFiltered() throws ApiException {

        mockHosts(
                new ApiHost().hostname("host-1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name("unrecognised health check").summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host-2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
        );

        DetailedHostStatuses result = underTest.getDetailedHostStatuses(Optional.of(RUNTIME));

        assertThat(result.isHostHealthy(hostName("host-1"))).isTrue();
    }

    @Test
    void testUnhealthyHostsAndCertificates() throws ApiException {

        mockHosts(
                new ApiHost().hostname("host-1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.CONCERNING)
                                .explanation("Expiring"))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-1").serviceName("service-1").healthSummary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-2").serviceName("service-2").healthSummary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host-2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-1").serviceName("service-1").healthSummary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-2").serviceName("service-2").healthSummary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host-3")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host-4")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.CONCERNING))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host-5")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-1").serviceName("bad-service-1").healthSummary(ApiHealthSummary.BAD))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-2").serviceName("bad-service-2").healthSummary(ApiHealthSummary.CONCERNING))
        );

        DetailedHostStatuses result = underTest.getDetailedHostStatuses(Optional.of(RUNTIME));

        assertThat(result.isCertExpiring(hostName("host-1"))).isTrue();
        assertThat(result.getCertHealth().get(hostName("host-1")).get().getExplanation()).isEqualTo("Expiring");
        assertThat(result.isHostHealthy(hostName("host-2"))).isTrue();
        assertThat(result.isHostHealthy(hostName("host-1"))).isTrue();
        assertThat(result.isHostHealthy(hostName("host-3"))).isFalse();
        assertThat(result.isHostHealthy(hostName("host-4"))).isFalse();
        assertThat(result.isHostHealthy(hostName("host-5"))).isFalse();
        assertThat(result.getServicesHealth().get(hostName("host-5"))
                .get().getServicesWithBadHealth()).hasSameElementsAs(Arrays.asList("bad-service-1", "bad-service-2"));
    }

    @Test
    void testHostsDecommissioned() throws ApiException {

        ApiHost host1 = new ApiHost().hostname("host-1")
                .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD));

        ApiHost host3 = new ApiHost().hostname("host-3")
                .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD));

        mockHosts(
                host1,
                new ApiHost().hostname("host-2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD)),
                host3
        );
        host1.setCommissionState(ApiCommissionState.DECOMMISSIONED);
        host3.setCommissionState(ApiCommissionState.DECOMMISSIONED);

        DetailedHostStatuses result = underTest.getDetailedHostStatuses(Optional.of(RUNTIME));

        assertThat(result.isHostDecommissioned(hostName("host-1"))).isTrue();
        assertThat(result.isHostDecommissioned(hostName("host-3"))).isTrue();
        assertThat(result.isHostDecommissioned(hostName("host-2"))).isFalse();
    }

    @Test
    void testHostsInMaintenanceMode() throws ApiException {

        mockHosts(
                new ApiHost().hostname("host-1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .maintenanceMode(Boolean.TRUE),
                new ApiHost().hostname("host-2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .maintenanceMode(Boolean.FALSE),
                new ApiHost().hostname("host-3")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .maintenanceMode(null),
                new ApiHost().hostname("host-4")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
        );

        DetailedHostStatuses result = underTest.getDetailedHostStatuses(Optional.of(RUNTIME));

        assertThat(result.isHostInMaintenanceMode(hostName("host-1"))).isTrue();
        assertThat(result.isHostInMaintenanceMode(hostName("host-2"))).isFalse();
        assertThat(result.isHostInMaintenanceMode(hostName("host-3"))).isFalse();
        assertThat(result.isHostInMaintenanceMode(hostName("host-4"))).isFalse();
    }

    @Test
    void testServicesNotRunningOnHosts() throws ApiException {

        mockHosts(
                new ApiHost().hostname("host-1")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-1").serviceName("service-1").healthSummary(ApiHealthSummary.GOOD)
                                .roleStatus(ApiRoleState.STOPPED))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-2").serviceName("service-2").healthSummary(ApiHealthSummary.GOOD)),
                new ApiHost().hostname("host-2")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
                        .addRoleRefsItem(new ApiRoleRef().roleName("role-1").serviceName("service-1").healthSummary(ApiHealthSummary.GOOD)
                                .roleStatus(ApiRoleState.STOPPING)),
                new ApiHost().hostname("host-3")
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(HOST_AGENT_CERTIFICATE_EXPIRY).summary(ApiHealthSummary.GOOD))
        );

        DetailedHostStatuses result = underTest.getDetailedHostStatuses(Optional.of(RUNTIME));

        assertThat(result.areServicesNotRunning(hostName("host-1"))).isTrue();
        assertThat(result.areServicesNotRunning(hostName("host-2"))).isTrue();
        assertThat(result.areServicesNotRunning(hostName("host-3"))).isFalse();
    }

    @Test
    void testCollectDisconnectedNodeManagers() throws ApiException {
        underTest = spy(underTest);

        doReturn("yarn").when(underTest).extractYarnServiceNameFromBlueprint(any(StackDtoDelegate.class));
        mockRoles(
                new ApiRole().name("resourcemanager").type(RESOURCEMANAGER).hostRef(new ApiHostRef().hostname("master0")).healthSummary(ApiHealthSummary.GOOD)
                        .addHealthChecksItem(new ApiHealthCheck().name(RESOURCE_MANAGER_FILE_DESCRIPTOR).summary(ApiHealthSummary.GOOD)),
                new ApiRole().name("nodemanager-compute0").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compute0")).healthSummary(ApiHealthSummary.GOOD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.GOOD)),
                new ApiRole().name("nodemanager-compute1").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compute1")).healthSummary(ApiHealthSummary.BAD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.BAD)),
                new ApiRole().name("nodemanager-compute2").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compuTe2")).healthSummary(ApiHealthSummary.BAD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(NODEMANAGER_LOG_DIRECTORIES_FREE_SPACE).summary(ApiHealthSummary.BAD)),
                new ApiRole().name("nodemanager-compute3").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("CoMpUtE3")).healthSummary(ApiHealthSummary.BAD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.BAD)),
                new ApiRole().name("nodemanager-compute4").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compute4")).healthSummary(ApiHealthSummary.GOOD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(NODEMANAGER_LOG_DIRECTORIES_FREE_SPACE).summary(ApiHealthSummary.GOOD))
        );

        Set<String> result = underTest.getDisconnectedNodeManagers();

        assertThat(result).hasSize(2).hasSameElementsAs(Set.of("compute1", "compute3"));
    }

    @Test
    void testCollectDisconnectedNodeManagersWithNullHealthChecks() throws ApiException {
        underTest = spy(underTest);

        doReturn("yarn").when(underTest).extractYarnServiceNameFromBlueprint(any(StackDtoDelegate.class));
        mockRoles(
                new ApiRole().name("resourcemanager").type(RESOURCEMANAGER).hostRef(new ApiHostRef().hostname("master0")).healthSummary(ApiHealthSummary.GOOD)
                        .addHealthChecksItem(new ApiHealthCheck().name(RESOURCE_MANAGER_FILE_DESCRIPTOR).summary(ApiHealthSummary.GOOD)),
                new ApiRole().name("nodemanager-compute0").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compute0")).healthSummary(ApiHealthSummary.GOOD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.GOOD)),
                new ApiRole().name("nodemanager-compute1").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compute1")).healthSummary(ApiHealthSummary.BAD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.BAD)),
                new ApiRole().name("nodemanager-compute2").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compuTe2")).healthSummary(ApiHealthSummary.BAD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(NODEMANAGER_LOG_DIRECTORIES_FREE_SPACE).summary(ApiHealthSummary.BAD)),
                new ApiRole().name("nodemanager-compute3").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("CoMpUtE3")).healthSummary(ApiHealthSummary.BAD)
                        .healthChecks(null),
                new ApiRole().name("nodemanager-compute4").type(NODEMANAGER).hostRef(new ApiHostRef().hostname("compute4")).healthSummary(ApiHealthSummary.GOOD)
                        .addHealthChecksItem(new ApiHealthCheck().name(NODE_MANAGER_CONNECTIVITY).summary(ApiHealthSummary.GOOD))
                        .addHealthChecksItem(new ApiHealthCheck().name(NODEMANAGER_LOG_DIRECTORIES_FREE_SPACE).summary(ApiHealthSummary.GOOD))
        );

        Set<String> result = underTest.getDisconnectedNodeManagers();

        assertThat(result).hasSize(1).hasSameElementsAs(Set.of("compute1"));
    }

    private void mockHosts(ApiHost... apiHosts) throws ApiException {
        Arrays.stream(apiHosts).forEach(apihost -> {
            apihost.setHealthSummary(ApiHealthSummary.GOOD);
            apihost.setCommissionState(ApiCommissionState.COMMISSIONED);
        });
        ApiHostList apiHostList = new ApiHostList().items(Arrays.asList(apiHosts));
        when(hostsResourceApi.readHosts(null, null, FULL_WITH_HEALTH_CHECK_EXPLANATION)).thenReturn(apiHostList);
    }

    private void mockRoles(ApiRole... roles) throws ApiException {
        Stream.of(roles).forEach(role -> {
            role.setCommissionState(ApiCommissionState.COMMISSIONED);
            role.setHealthSummary(ApiHealthSummary.GOOD);
        });
        ApiRoleList roleList = new ApiRoleList().items(Arrays.asList(roles));
        doReturn(roleList).when(rolesResourceApi).readRoles(eq(STACK_NAME), anyString(), any(), eq(FULL_WITH_HEALTH_CHECK_EXPLANATION));
    }
}