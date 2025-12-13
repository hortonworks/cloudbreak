package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommissionState;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerHostHealthyStatusCheckerTest {

    private static final String VIEW_TYPE = ClouderaManagerHostHealthyStatusChecker.VIEW_TYPE;

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private ClusterEventService clusterEventService;

    @Mock
    private HostsResourceApi hostsResourceApi;

    private ClouderaManagerHostHealthyStatusChecker underTest;

    @BeforeEach
    void init() {
        when(clouderaManagerApiPojoFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
    }

    @Test
    void testSingleGoodHostOnFirstInvocation() throws ApiException {
        Set<InstanceMetadataView> instanceMetadatas = Set.of(constructInstanceMetadata("h1", 1L));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, instanceMetadatas);

        ApiHostList apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, false, ApiCommissionState.COMMISSIONED)));

        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        boolean result = underTest.doStatusCheck(getPollerObject());
        assertTrue(result);
    }

    @Test
    void testHostNotMarkedAsGood() throws ApiException {
        Set<InstanceMetadataView> instanceMetadatas = Set.of(constructInstanceMetadata("h1", 1L));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, instanceMetadatas);

        ApiHostList apiHostList;
        boolean result;

        // Not a recent enough heartbeat
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start, ApiHealthSummary.GOOD, false, ApiCommissionState.COMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);

        // HealthSummary - CONCERNING
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.CONCERNING, false,
                        ApiCommissionState.COMMISSIONED, ApiHealthSummary.BAD, "HOST_AGENT_CERTIFICATE_EXPIRY")));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);

        // HealthSummary - BAD
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.BAD, false, ApiCommissionState.COMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
    }

    @Test
    void testHostMarkedAsGoodDespiteMaintenanceCommissionState() throws ApiException {
        Set<InstanceMetadataView> instanceMetadatas = Set.of(constructInstanceMetadata("h1", 1L));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, instanceMetadatas);

        ApiHostList apiHostList;
        boolean result;

        // maintenance mode, and DECOMMISSIONED
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject());
        assertTrue(result);

        // Commission state: DECOMMISSIONED
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, false, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject());
        assertTrue(result);

        // Commission state: UNKNOWN
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, false, ApiCommissionState.UNKNOWN)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject());
        assertTrue(result);
    }

    @Test
    void testSeriesOfInvocations() throws ApiException {
        Set<InstanceMetadataView> instanceMetadatas = Set.of(constructInstanceMetadata("h1", 1L),
                constructInstanceMetadata("h2", 2L),
                constructInstanceMetadata("h3", 3L));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, instanceMetadatas);
        ApiHostList apiHostList;
        boolean result;

        // First invocation - CM gives back info on only 1 of the hosts, and it is not in healthy state.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start, ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(3, underTest.hostnamesToCheckFor.size());

        // Second invocation - the same host moves into HEALTHY state
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(2, underTest.hostnamesToCheckFor.size());

        // Third invocation - 2nd hosts marked good, and 3rd is still not ready. 1st host continues to exist.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h3", underTest.start, ApiHealthSummary.CONCERNING, true, ApiCommissionState.DECOMMISSIONED)
                ));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(1, underTest.hostnamesToCheckFor.size());

        // Fourth invocation - all hosts considered healthy.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h3", underTest.start.plusMillis(300), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)
        ));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertTrue(result);
        assertEquals(0, underTest.hostnamesToCheckFor.size());
    }

    @Test
    void testSeriesOfInvocationsWithConcerningHealthNotAllowed() throws ApiException {
        Set<InstanceMetadataView> instanceMetadatas = Set.of(constructInstanceMetadata("h1", 1L),
                constructInstanceMetadata("h2", 2L),
                constructInstanceMetadata("h3", 3L));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, instanceMetadatas);
        ApiHostList apiHostList;
        boolean result;

        // First invocation - CM gives back info on only 1 of the hosts, and it is not in healthy state.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start, ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(3, underTest.hostnamesToCheckFor.size());

        // Second invocation - the same host moves into HEALTHY state
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(2, underTest.hostnamesToCheckFor.size());

        // Third invocation - 2nd  and 3rd is still not ready. 1st host continues to exist.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start, ApiHealthSummary.CONCERNING, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h3", underTest.start, ApiHealthSummary.CONCERNING, true, ApiCommissionState.DECOMMISSIONED)
        ));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(2, underTest.hostnamesToCheckFor.size());

        // Fourth invocation - 2 hosts are considered healthy except host2
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start.plusMillis(300), ApiHealthSummary.CONCERNING, true,
                        ApiCommissionState.DECOMMISSIONED, ApiHealthSummary.CONCERNING, "HOST_AGENT_LOG_DIRECTORY_FREE_SPACE"),
                constructApiHost("h3", underTest.start.plusMillis(300), ApiHealthSummary.CONCERNING, true,
                        ApiCommissionState.DECOMMISSIONED, ApiHealthSummary.CONCERNING, "HOST_AGENT_CERTIFICATE_EXPIRY")
        ));

        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(1, underTest.hostnamesToCheckFor.size());
    }

    @Test
    void testSeriesOfInvocationsWithConcerningHealth() throws ApiException {
        Set<InstanceMetadataView> instanceMetadatas = Set.of(constructInstanceMetadata("h1", 1L),
                constructInstanceMetadata("h2", 2L),
                constructInstanceMetadata("h3", 3L));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, instanceMetadatas);
        ApiHostList apiHostList;
        boolean result;

        // First invocation - CM gives back info on only 1 of the hosts, and it is not in healthy state.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start, ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(3, underTest.hostnamesToCheckFor.size());

        // Second invocation - the same host moves into HEALTHY state
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(2, underTest.hostnamesToCheckFor.size());

        // Third invocation - 2nd hosts marked good, and 3rd is still not ready. 1st host continues to exist.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h3", underTest.start, ApiHealthSummary.CONCERNING, true, ApiCommissionState.DECOMMISSIONED)
        ));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertFalse(result);
        assertEquals(1, underTest.hostnamesToCheckFor.size());

        // Fourth invocation - all hosts considered healthy.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h3", underTest.start.plusMillis(300), ApiHealthSummary.CONCERNING, true,
                        ApiCommissionState.DECOMMISSIONED, ApiHealthSummary.CONCERNING, "HOST_AGENT_CERTIFICATE_EXPIRY")
        ));

        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject());
        assertTrue(result);
        assertEquals(0, underTest.hostnamesToCheckFor.size());
    }

    private ClouderaManagerCommandPollerObject getPollerObject() {
        Stack stack = new Stack();
        stack.setId(1L);
        return new ClouderaManagerCommandPollerObject(stack, new ApiClient(), BigDecimal.ONE);
    }

    private ApiHost constructApiHost(String hostname, Instant lastHeartbeat, ApiHealthSummary healthSummary,
            boolean maintenanceMode, ApiCommissionState commissionState) {
        return new ApiHost()
                .hostname(hostname)
                .lastHeartbeat(lastHeartbeat.toString())
                .healthSummary(healthSummary)
                .maintenanceMode(maintenanceMode)
                .commissionState(commissionState);
    }

    private ApiHost constructApiHost(String hostname, Instant lastHeartbeat, ApiHealthSummary healthSummary,
            boolean maintenanceMode, ApiCommissionState commissionState, ApiHealthSummary apiHealthSummary, String apiHealthCheckName) {
        ApiHealthCheck apiHealthCheck = new ApiHealthCheck();
        apiHealthCheck.setName(apiHealthCheckName);
        apiHealthCheck.setSummary(apiHealthSummary);
        return new ApiHost()
                .hostname(hostname)
                .lastHeartbeat(lastHeartbeat.toString())
                .healthSummary(healthSummary)
                .maintenanceMode(maintenanceMode)
                .healthChecks(List.of(apiHealthCheck))
                .commissionState(commissionState);
    }

    private InstanceMetaData constructInstanceMetadata(String discoveryName, Long privateId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(discoveryName);
        instanceMetaData.setPrivateId(privateId);
        return instanceMetaData;
    }
}
