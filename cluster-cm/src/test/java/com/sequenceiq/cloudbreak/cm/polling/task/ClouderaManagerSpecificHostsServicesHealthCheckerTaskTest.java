package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerHealthService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerSpecificHostsServicesHealthCheckerTaskTest {

    private static final Optional<String> RUNTIME_VERSION = Optional.of("7.3.1");

    private static final String HEALTHY_HOST = "host1.example.com";

    private static final long HOST_PRIVATE_ID = 42L;

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private ClusterEventService clusterEventService;

    @Mock
    private ClouderaManagerHealthService clouderaManagerHealthService;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private ApiClient apiClient;

    private InstanceMetadataView hostWithFqdn;

    private InstanceMetadataView hostWithoutFqdn;

    private ClouderaManagerSpecificHostsServicesHealthCheckerTask underTest;

    @BeforeEach
    void setUp() {
        hostWithFqdn = mockInstance(HEALTHY_HOST);
        hostWithoutFqdn = mockInstance(null);
        underTest = new ClouderaManagerSpecificHostsServicesHealthCheckerTask(clouderaManagerApiPojoFactory, clusterEventService,
                clouderaManagerHealthService, RUNTIME_VERSION, Set.of(hostWithFqdn, hostWithoutFqdn));
    }

    @Test
    void testDoStatusCheckReturnsTrueWhenAllSpecifiedHostsHealthy() throws ApiException {
        when(clouderaManagerHealthService.getExtendedHostStatuses(eq(apiClient), eq(RUNTIME_VERSION)))
                .thenReturn(new ExtendedHostStatuses(Map.of(
                        HostName.hostName(HEALTHY_HOST),
                        Set.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.HEALTHY, Optional.empty(), Optional.empty()))
                )));

        boolean result = underTest.doStatusCheck(new ClouderaManagerPollerObject(stack, apiClient));

        assertTrue(result);
    }

    @Test
    void testDoStatusCheckReturnsFalseWhenSpecifiedHostUnhealthy() throws ApiException {
        when(clouderaManagerHealthService.getExtendedHostStatuses(eq(apiClient), eq(RUNTIME_VERSION)))
                .thenReturn(new ExtendedHostStatuses(Map.of(
                        HostName.hostName(HEALTHY_HOST),
                        Set.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.UNHEALTHY, Optional.of("bad"), Optional.empty()))
                )));
        when(hostWithFqdn.getPrivateId()).thenReturn(HOST_PRIVATE_ID);

        boolean result = underTest.doStatusCheck(new ClouderaManagerPollerObject(stack, apiClient));

        assertFalse(result);
        assertEquals(Set.of(HOST_PRIVATE_ID), underTest.getFailedInstancePrivateIds());
        assertEquals(Set.of(HOST_PRIVATE_ID), underTest.getFailedInstancePrivateIds());
    }

    @Test
    void testDoStatusCheckReturnsFalseWhenSpecifiedHostMissingFromCm() throws ApiException {
        when(clouderaManagerHealthService.getExtendedHostStatuses(eq(apiClient), eq(RUNTIME_VERSION)))
                .thenReturn(new ExtendedHostStatuses(Map.of(
                        HostName.hostName("another-host.example.com"),
                        Set.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.HEALTHY, Optional.empty(), Optional.empty()))
                )));
        when(hostWithFqdn.getPrivateId()).thenReturn(HOST_PRIVATE_ID);

        boolean result = underTest.doStatusCheck(new ClouderaManagerPollerObject(stack, apiClient));

        assertFalse(result);
    }

    @Test
    void testGetFailedInstancePrivateIdsReturnsEmptySetWhenHealthy() throws ApiException {
        when(clouderaManagerHealthService.getExtendedHostStatuses(eq(apiClient), eq(RUNTIME_VERSION)))
                .thenReturn(new ExtendedHostStatuses(Map.of(
                        HostName.hostName(HEALTHY_HOST),
                        Set.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.HEALTHY, Optional.empty(), Optional.empty()))
                )));

        boolean result = underTest.doStatusCheck(new ClouderaManagerPollerObject(stack, apiClient));

        assertTrue(result);
        assertTrue(underTest.getFailedInstancePrivateIds().isEmpty());
    }

    private InstanceMetadataView mockInstance(String fqdn) {
        InstanceMetadataView instance = mock(InstanceMetadataView.class);
        if (fqdn != null) {
            when(instance.getDiscoveryFQDN()).thenReturn(fqdn);
        }
        return instance;
    }
}

