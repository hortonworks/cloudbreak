package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class InstanceMetadataUpdaterTest {

    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private HostMetadataService hostMetadataService;

    @InjectMocks
    private InstanceMetadataUpdater underTest;

    @Before
    public void setUp() throws CloudbreakException, JsonProcessingException, CloudbreakOrchestratorFailedException {
        MockitoAnnotations.initMocks(this);
        when(hostOrchestratorResolver.get(anyString())).thenReturn(hostOrchestrator);
        when(gatewayConfigService.getGatewayConfig(any(Stack.class), any(InstanceMetaData.class), anyBoolean())).thenReturn(gatewayConfig);
        when(cloudbreakMessagesService.getMessage(anyString(), anyCollection())).thenReturn("message");

        InstanceMetadataUpdater.Package packageByName = new InstanceMetadataUpdater.Package();
        packageByName.setName("packageByName");
        packageByName.setPkgName(Lists.newArrayList("packageByName"));
        InstanceMetadataUpdater.Package packageByCmd = new InstanceMetadataUpdater.Package();
        packageByCmd.setName("packageByCmd");
        packageByCmd.setPkgName(Lists.newArrayList("packageByCmd"));

        underTest.setPackages(Lists.newArrayList(packageByCmd, packageByName));

        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        hostPackageMap.put("hostByCmd", packageMap());
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);
    }

    private Stack createStack() throws JsonProcessingException {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCluster(new Cluster());
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("salt");
        stack.setOrchestrator(orchestrator);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(createInstanceGroup("instanceId", InstanceGroupType.GATEWAY));
        instanceGroups.add(createInstanceGroup("hostByCmd", InstanceGroupType.CORE));
        stack.setInstanceGroups(instanceGroups);
        return stack;
    }

    @Test
    public void updatePackageVersionsOnAllInstances() throws Exception {
        underTest.updatePackageVersionsOnAllInstances(createStack());

        verify(cloudbreakEventService, times(0)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
    }

    @Test
    public void updatePackageVersionsOnAllInstancesInstancePkgQueryFailed() throws Exception {
        Stack stack = createStack();

        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        hostPackageMap.put("hostByCmd", falsePackageMap());
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);

        underTest.updatePackageVersionsOnAllInstances(stack);

        verify(hostMetadataService, times(1)).updateHostMetaDataStatus(any(), anyString(), eq(HostMetadataState.UNHEALTHY));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        verify(cloudbreakMessagesService, times(1))
                .getMessage(eq(InstanceMetadataUpdater.Msg.PACKAGE_VERSION_CANNOT_BE_QUERIED.code()), anyCollection());
        assertEquals(InstanceStatus.ORCHESTRATION_FAILED, stack.getInstanceGroups().stream()
            .filter(instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream()
                .filter(instanceMetaData -> StringUtils.equals(instanceMetaData.getDiscoveryFQDN(), "hostByCmd"))
                .findFirst()
                .isPresent())
            .findFirst()
            .get().getInstanceMetaDataSet().iterator().next().getInstanceStatus());
    }

    @Test
    public void updatePackageVersionsOnAllInstancesInstanceMissingPackageVersion() throws Exception {
        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        Map<String, String> packageMap = packageMap();
        packageMap.remove("packageByName");
        hostPackageMap.put("hostByCmd", packageMap);
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);

        underTest.updatePackageVersionsOnAllInstances(createStack());

        verify(cloudbreakEventService, times(2)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        verify(cloudbreakMessagesService, times(1))
                .getMessage(eq(InstanceMetadataUpdater.Msg.PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING.code()), anyCollection());
        verify(cloudbreakMessagesService, times(1))
                .getMessage(eq(InstanceMetadataUpdater.Msg.PACKAGE_VERSIONS_ARE_CHANGED.code()), anyCollection());
    }

    @Test
    public void updatePackageVersionsOnAllInstancesDifferentVersion() throws Exception {
        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        Map<String, String> packageMap = packageMap();
        packageMap.put("packageByName", "2");
        hostPackageMap.put("hostByCmd", packageMap);
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);

        underTest.updatePackageVersionsOnAllInstances(createStack());

        verify(cloudbreakEventService, times(2)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        verify(cloudbreakMessagesService, times(1))
                .getMessage(eq(InstanceMetadataUpdater.Msg.PACKAGES_ON_INSTANCES_ARE_DIFFERENT.code()), anyCollection());
        verify(cloudbreakMessagesService, times(1))
                .getMessage(eq(InstanceMetadataUpdater.Msg.PACKAGE_VERSIONS_ARE_CHANGED.code()), anyCollection());
    }

    private Map<String, String> packageMap() {
        Map<String, String> packageMap = Maps.newHashMap();
        packageMap.put("packageByName", "1");
        packageMap.put("packageByCmd", "1");
        return packageMap;
    }

    private Map<String, String> falsePackageMap() {
        Map<String, String> packageMap = Maps.newHashMap();
        packageMap.put("packageByName", "false");
        packageMap.put("packageByCmd", "false");
        return packageMap;
    }

    private InstanceGroup createInstanceGroup(String instanceId, InstanceGroupType instanceGroupType) throws JsonProcessingException {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        Image image = new Image("imagename", null, "os", "ostype", "catalogurl",
                "catalogname", "iamgeid", packageMap());
        instanceMetaData.setImage(new Json(image));
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setDiscoveryFQDN(instanceId);
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));
        return instanceGroup;
    }
}
