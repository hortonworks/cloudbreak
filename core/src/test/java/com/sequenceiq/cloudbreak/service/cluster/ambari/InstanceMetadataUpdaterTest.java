package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.cluster.Package;
import com.sequenceiq.cloudbreak.service.cluster.PackageName;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class InstanceMetadataUpdaterTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private StackService stackService;

    @InjectMocks
    private InstanceMetadataUpdater underTest;

    @BeforeEach
    public void setUp() throws CloudbreakException, JsonProcessingException, CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any(Stack.class))).thenReturn(gatewayConfig);

        Package packageByName = new Package();
        packageByName.setName("packageByName");
        packageByName.setPkg(Lists.newArrayList(generatePackageName("packageByName", "(.*)-(.*)")));
        Package packageByCmd = new Package();
        packageByCmd.setName("packageByCmd");
        packageByCmd.setPkg(Lists.newArrayList(generatePackageName("packageByCmd", null)));

        underTest.setPackages(Lists.newArrayList(packageByCmd, packageByName));

        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        hostPackageMap.put("hostByCmd", packageMap());
        lenient().when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);
    }

    private Stack createStack() {
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
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(createStack());
        underTest.updatePackageVersionsOnAllInstances(1L);

        verify(cloudbreakEventService, times(0)).fireCloudbreakEvent(anyLong(), anyString(), any(ResourceEvent.class));
    }

    @Test
    public void updatePackageVersionsOnAllInstancesInstancePkgQueryFailed() throws Exception {
        Stack stack = createStack();

        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        hostPackageMap.put("hostByCmd", falsePackageMap());
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        underTest.updatePackageVersionsOnAllInstances(1L);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(),
                eq(ResourceEvent.CLUSTER_PACKAGE_VERSION_CANNOT_BE_QUERIED), anyCollection());
        verify(cloudbreakEventService, times(2)).fireCloudbreakEvent(anyLong(), anyString(),
                any(ResourceEvent.class), anyCollection());
        assertEquals(SERVICES_UNHEALTHY, stack.getInstanceGroups().stream()
                .filter(instanceGroup -> instanceGroup.getInstanceMetaData().stream()
                        .anyMatch(instanceMetaData -> StringUtils.equals(instanceMetaData.getDiscoveryFQDN(), "hostByCmd")))
                .findFirst()
                .get().getInstanceMetaData().iterator().next().getInstanceStatus());
    }

    @Test
    public void updatePackageVersionsOnAllInstancesInstanceMissingPackageVersion() throws Exception {
        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        Map<String, String> packageMap = packageMap();
        packageMap.remove("packageByName");
        hostPackageMap.put("hostByCmd", packageMap);
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(createStack());
        underTest.updatePackageVersionsOnAllInstances(1L);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(),
                eq(ResourceEvent.CLUSTER_PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING), anyCollection());
    }

    @Test
    public void updatePackageVersionsOnAllInstancesDifferentVersion() throws Exception {
        Map<String, Map<String, String>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put("instanceId", packageMap());
        Map<String, String> packageMap = packageMap();
        packageMap.put("packageByName", "2");
        hostPackageMap.put("hostByCmd", packageMap);
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(createStack());
        underTest.updatePackageVersionsOnAllInstances(1L);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(),
                eq(ResourceEvent.CLUSTER_PACKAGES_ON_INSTANCES_ARE_DIFFERENT), anyCollection());
    }

    private Map<String, String> packageMap() {
        Map<String, String> packageMap = Maps.newHashMap();
        packageMap.put("packageByName", "1-1");
        packageMap.put("packageByCmd", "1");
        return packageMap;
    }

    private Map<String, String> falsePackageMap() {
        Map<String, String> packageMap = Maps.newHashMap();
        packageMap.put("packageByName", "false");
        packageMap.put("packageByCmd", "false");
        return packageMap;
    }

    private InstanceGroup createInstanceGroup(String instanceId, InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        Image image = new Image("imagename", null, "os", "ostype", "arch", "catalogurl",
                "catalogname", "iamgeid", packageMap(), null, null, null);
        instanceMetaData.setImage(new Json(image));
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setDiscoveryFQDN(instanceId);
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));
        return instanceGroup;
    }

    private PackageName generatePackageName(String pkg, String pattern) {
        PackageName packageName = new PackageName();
        packageName.setName(pkg);
        packageName.setPattern(pattern);
        return packageName;
    }
}
