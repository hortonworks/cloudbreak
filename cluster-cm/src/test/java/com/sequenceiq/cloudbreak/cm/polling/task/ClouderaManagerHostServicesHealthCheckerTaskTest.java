package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
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

@ExtendWith(MockitoExtension.class)
class ClouderaManagerHostServicesHealthCheckerTaskTest {

    private ClouderaManagerHostServicesHealthCheckerTask underTest;

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

    @BeforeEach
    void setUp() {
        underTest = new ClouderaManagerHostServicesHealthCheckerTask(clouderaManagerApiPojoFactory, clusterEventService, clouderaManagerHealthService,
                Optional.of("7.3.1"));
    }

    @Test
    void testDoStatusCheckWhenNoFailedHostsFouns() throws ApiException {
        when(clouderaManagerHealthService.getExtendedHostStatuses(eq(apiClient), any()))
                .thenReturn(new ExtendedHostStatuses(Map.of(HostName.hostName("host1"),
                        Set.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.HEALTHY, Optional.empty())))));
        boolean result = underTest.doStatusCheck(new ClouderaManagerPollerObject(stack, apiClient));
        assertTrue(result);
    }

    @Test
    void testDoStatusCheckWhenFailedHostsFouns() throws ApiException {
        when(clouderaManagerHealthService.getExtendedHostStatuses(eq(apiClient), any()))
                .thenReturn(new ExtendedHostStatuses(Map.of(HostName.hostName("host1"),
                        Set.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.UNHEALTHY, Optional.of("error"))))));
        boolean result = underTest.doStatusCheck(new ClouderaManagerPollerObject(stack, apiClient));
        assertFalse(result);
    }
}