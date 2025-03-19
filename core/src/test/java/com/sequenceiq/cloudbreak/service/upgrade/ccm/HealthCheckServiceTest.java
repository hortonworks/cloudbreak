package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class HealthCheckServiceTest {

    private static final Long STACK_ID = 2L;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private Stack stack;

    @InjectMocks
    private HealthCheckService underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
                // Testcase name, Health Map, Expected result
                { "No host", Map.of(HostName.hostName("host1"), Set.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.UNHEALTHY, Optional.of("some insignificant error"),Optional.empty()))), Set.of() },
                { "One host unhealthy", Map.of(HostName.hostName("host1"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.of("some significant error"),Optional.empty()))), Set.of("host1") },
                { "One host healthy", Map.of(HostName.hostName("host1"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.HEALTHY, Optional.empty(),Optional.empty()))), Set.of() },
                { "One host healthy, one unhealthy", Map.of(HostName.hostName("host1"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.of("some significant error"),Optional.empty())),
                                                            HostName.hostName("host2"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.HEALTHY, Optional.empty(),Optional.empty()))), Set.of("host1") },
                { "Two hosts healthy", Map.of(HostName.hostName("host1"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.HEALTHY, Optional.empty(),Optional.empty())),
                                              HostName.hostName("host2"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.HEALTHY, Optional.empty(),Optional.empty()))), Set.of() },
                { "Two hosts unhealthy", Map.of(HostName.hostName("host1"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.of("some significant error"),Optional.empty())),
                                                HostName.hostName("host2"), Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.empty(),Optional.empty()))), Set.of("host1", "host2") },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void healthCheck(String testCaseName, Map<HostName, Set<HealthCheck>> hostsHealth, Set<String> expected) {
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(stack.getCluster()).thenReturn(new Cluster());
        when(clusterApiConnectors.getConnector(nullable(Stack.class))).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        ExtendedHostStatuses statuses = new ExtendedHostStatuses(hostsHealth);
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(statuses);
        Set<String> result = underTest.getUnhealthyHosts(STACK_ID);
        assertThat(result).hasSameElementsAs(expected);
    }
}
