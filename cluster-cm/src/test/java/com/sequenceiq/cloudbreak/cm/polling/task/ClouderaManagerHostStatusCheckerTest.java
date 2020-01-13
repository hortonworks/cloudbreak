package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerHostStatusCheckerTest {

    private static final String VIEWTYPE = "FULL";

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private HostsResourceApi hostsResourceApi;

    private ClouderaManagerHostStatusChecker underTest;

    @Before
    public void init() {
        underTest = new ClouderaManagerHostStatusChecker(clouderaManagerApiPojoFactory, cloudbreakEventService);
        when(clouderaManagerApiPojoFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
    }

    @Test
    public void shouldBeFalseWhenNoHostsReturned() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(Collections.emptyList()));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData), new CommandsResourceApi());

        assertFalse(result);
    }

    @Test
    public void shouldBeFalseWhenHostsReturnedHasNoHeartbeat() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = new ApiHost().ipAddress(instanceMetaData.getPrivateIp());
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData), new CommandsResourceApi());

        assertFalse(result);
    }

    @Test
    public void shouldBeFalseWhenHostsReturnedHasOldHeartbeat() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = new ApiHost()
                .ipAddress(instanceMetaData.getPrivateIp())
                .lastHeartbeat(Instant.now().minus(5, ChronoUnit.MINUTES).toString());
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData), new CommandsResourceApi());

        assertFalse(result);
    }

    @Test
    public void shouldBeTrueWhenHostsReturnedHasRecentHeartbeat() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData), new CommandsResourceApi());

        assertTrue(result);
    }

    @Test
    public void shouldBeFalseWhenHostsReturnedHasDifferentIp() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        ApiHost apiHost = new ApiHost()
                .ipAddress("2.2.2.2")
                .lastHeartbeat(Instant.now().plus(5, ChronoUnit.MINUTES).toString());
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData), new CommandsResourceApi());

        assertFalse(result);
    }

    @Test
    public void shouldBeFalseWhenHostsHasMissingHost() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2), new CommandsResourceApi());

        assertFalse(result);
    }

    @Test
    public void shouldBeTrueWhenMultipleValidHosts() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        ApiHost apiHost2 = getValidApiHost(instanceMetaData2);
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost, apiHost2)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2), new CommandsResourceApi());

        assertTrue(result);
    }

    @Test
    public void shouldBeFalseWhenOneHostHasDifferentIp() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        ApiHost apiHost2 = getValidApiHost(instanceMetaData2).ipAddress("3.3.3.3");
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost, apiHost2)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2), new CommandsResourceApi());

        assertFalse(result);
    }

    @Test
    public void shouldBeTrueWhenOneInstanceHasNoDiscoveryFqdn() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setDiscoveryFQDN(null);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2), new CommandsResourceApi());

        assertTrue(result);
    }

    @Test
    public void shouldBeTrueWhenOneInstanceIsTerminated() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.TERMINATED);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2), new CommandsResourceApi());

        assertTrue(result);
    }

    @Test
    public void shouldBeTrueWhenOneInstanceIsDeletedOnProvider() throws ApiException {
        InstanceMetaData instanceMetaData = validInstanceMetadata();
        InstanceMetaData instanceMetaData2 = validInstanceMetadata();
        instanceMetaData2.setPrivateIp("2.2.2.2");
        instanceMetaData2.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        ApiHost apiHost = getValidApiHost(instanceMetaData);
        when(hostsResourceApi.readHosts(VIEWTYPE)).thenReturn(new ApiHostList().items(List.of(apiHost)));

        boolean result = underTest.doStatusCheck(getPollerObject(instanceMetaData, instanceMetaData2), new CommandsResourceApi());

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

    private ClouderaManagerPollerObject getPollerObject(InstanceMetaData... instanceMetaDatas) {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaDatas));
        stack.setInstanceGroups(Set.of(instanceGroup));
        return new ClouderaManagerPollerObject(stack, new ApiClient(), BigDecimal.ONE);
    }

}