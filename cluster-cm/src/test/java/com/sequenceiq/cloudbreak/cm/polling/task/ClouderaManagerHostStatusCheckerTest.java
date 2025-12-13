package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerHostStatusCheckerTest {

    private static final String VIEWTYPE = "FULL";

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private ClusterEventService clusterEventService;

    @Mock
    private HostsResourceApi hostsResourceApi;

    private ClouderaManagerHostStatusChecker underTest;

    @BeforeEach
    void init() {
        underTest = new ClouderaManagerHostStatusChecker(clouderaManagerApiPojoFactory, clusterEventService, false);
        when(clouderaManagerApiPojoFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
    }

    @Test
    void shouldBeFalseWhenNoHostsReturned() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList());

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData));

        assertFalse(result);
    }

    @Test
    void shouldBeFalseWhenHostsReturnedHasNoHeartbeat() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = new ApiHost().ipAddress(instanceMetaData.getPrivateIp());
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData));

        assertFalse(result);
    }

    @Test
    void shouldBeFalseWhenHostsReturnedHasOldHeartbeat() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = new ApiHost()
                .ipAddress(instanceMetaData.getPrivateIp())
                .lastHeartbeat(Instant.now().minus(5, ChronoUnit.MINUTES).toString());
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData));

        assertFalse(result);
    }

    @Test
    void shouldBeTrueWhenHostsReturnedHasRecentHeartbeat() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData));

        assertTrue(result);
    }

    @Test
    void shouldBeFalseWhenHostsReturnedHasDifferentIp() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = new ApiHost()
                .ipAddress("2.2.2.2")
                .lastHeartbeat(Instant.now().plus(5, ChronoUnit.MINUTES).toString());
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData));

        assertFalse(result);
    }

    @Test
    void shouldBeFalseWhenHostsHasMissingHost() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertFalse(result);
    }

    @Test
    void shouldBeTrueWhenMultipleValidHosts() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        ApiHost apiHost2 = getValidApiHost(instanceMetaData2);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost, apiHost2));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    @Test
    void shouldBeFalseWhenOneHostHasDifferentIp() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        ApiHost apiHost2 = getValidApiHost(instanceMetaData2).ipAddress("3.3.3.3");
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost, apiHost2));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertFalse(result);
    }

    @Test
    void shouldBeTrueWhenOneInstanceHasNoDiscoveryFqdn() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setDiscoveryFQDN(null);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    @Test
    void shouldBeTrueWhenOneInstanceIsTerminated() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.TERMINATED);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    @Test
    void shouldBeTrueWhenServicesOnOneInstanceAreUnhealthy() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        ApiHost apiHost2 = getValidApiHost(instanceMetaData2);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost, apiHost2));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    @Test
    void shouldBeTrueWhenOneInstanceIsDeletedOnProvider() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    @Test
    void shouldBeTrueWhenOneInstanceIsStopped() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.STOPPED);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    @Test
    void shouldBeTrueWhenOneInstanceIsFailed() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.FAILED);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    @Test
    void shouldBeTrueWhenOneInstanceIsOrchestrationFailed() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.ORCHESTRATION_FAILED);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(null, null, VIEWTYPE)).thenReturn(newApiHostList(apiHost));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2));

        assertTrue(result);
    }

    private ApiHost getValidApiHost(InstanceMetaData instanceMetaData) {
        return new ApiHost()
                    .ipAddress(instanceMetaData.getPrivateIp())
                    .lastHeartbeat(Instant.now().plus(5, ChronoUnit.MINUTES).toString());
    }

    private InstanceMetaData validInstanceMetadata() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("ins1");
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        instanceMetaData.setPrivateIp("1.1.1.1");
        return instanceMetaData;
    }

    private ClouderaManagerCommandPollerObject getPollerObject(InstanceMetaData... instanceMetaDatas) {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaDatas));
        stack.setInstanceGroups(Set.of(instanceGroup));
        return new ClouderaManagerCommandPollerObject(stack, new ApiClient(), BigDecimal.ONE);
    }

    private ApiHostList newApiHostList(ApiHost... apiHosts) {
        return new ApiHostList().items(Arrays.stream(apiHosts).toList());
    }

}