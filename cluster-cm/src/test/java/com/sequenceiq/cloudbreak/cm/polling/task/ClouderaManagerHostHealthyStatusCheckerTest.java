package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommissionState;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerHostHealthyStatusCheckerTest {

    private static final String VIEW_TYPE = ClouderaManagerHostHealthyStatusChecker.VIEW_TYPE;

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private ClusterEventService clusterEventService;

    @Mock
    private HostsResourceApi hostsResourceApi;

    private ClouderaManagerHostHealthyStatusChecker underTest;

    @Before
    public void init() {
        when(clouderaManagerApiPojoFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
    }

    @Test
    public void testSingleGoodHostOnFirstInvocation() throws ApiException {
        Set<String> hostnamesToCheckFor = Sets.newHashSet(Arrays.asList("h1"));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, hostnamesToCheckFor);

        ApiHostList apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, false, ApiCommissionState.COMMISSIONED)));

        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        boolean result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(true, result);
    }

    @Test
    public void testHostNotMarkedAsGood() throws ApiException {
        Set<String> hostnamesToCheckFor = Sets.newHashSet(Arrays.asList("h1"));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, hostnamesToCheckFor);

        ApiHostList apiHostList;
        boolean result;

        // Not a recent enough heartbeat
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start, ApiHealthSummary.GOOD, false, ApiCommissionState.COMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(false, result);

        // HealthSummary - CONCERNING
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.CONCERNING, false, ApiCommissionState.COMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(false, result);

        // HealthSummary - BAD
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.BAD, false, ApiCommissionState.COMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(false, result);
    }

    @Test
    public void testHostMarkedAsGoodDespiteMaintenanceCommissionState() throws ApiException {
        Set<String> hostnamesToCheckFor = Sets.newHashSet(Arrays.asList("h1"));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, hostnamesToCheckFor);

        ApiHostList apiHostList;
        boolean result;

        // maintenance mode, and DECOMMISSIONED
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(true, result);

        // Commission state: DECOMMISSIONED
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, false, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(true, result);

        // Commission state: UNKNOWN
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, false, ApiCommissionState.UNKNOWN)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);

        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(true, result);
    }

    @Test
    public void testSeriesOfInvocations() throws ApiException {
        Set<String> hostnamesToCheckFor = Sets.newHashSet(Arrays.asList("h1", "h2", "h3"));
        underTest = new ClouderaManagerHostHealthyStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, hostnamesToCheckFor);
        ApiHostList apiHostList;
        boolean result;

        // First invocation - CM gives back info on only 1 of the hosts, and it is not in healthy state.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start, ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(false, result);
        assertEquals(3, underTest.hostnamesToCheckFor.size());

        // Second invocation - the same host moves into HEALTHY state
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(false, result);
        assertEquals(2, underTest.hostnamesToCheckFor.size());

        // Third invocation - 2nd hosts marked good, and 3rd is still not ready. 1st host continues to exist.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h3", underTest.start, ApiHealthSummary.CONCERNING, true, ApiCommissionState.DECOMMISSIONED)
                ));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(false, result);
        assertEquals(1, underTest.hostnamesToCheckFor.size());

        // Fourth invocation - all hosts considered healthy.
        apiHostList = new ApiHostList().items(List.of(
                constructApiHost("h1", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h2", underTest.start.plusMillis(1), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED),
                constructApiHost("h3", underTest.start.plusMillis(300), ApiHealthSummary.GOOD, true, ApiCommissionState.DECOMMISSIONED)
        ));
        when(hostsResourceApi.readHosts(null, null, VIEW_TYPE)).thenReturn(apiHostList);
        result = underTest.doStatusCheck(getPollerObject(), mock(CommandsResourceApi.class));
        assertEquals(true, result);
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
}
