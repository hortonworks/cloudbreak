package com.sequenceiq.cloudbreak.service.cluster.ambari;

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

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;

public class InstanceMetadataUpdaterTest {

    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private InstanceMetadataUpdater underTest;

    private Stack stack;

    private String packageByName;

    private String packageByCmd;

    @Before
    public void setUp() throws CloudbreakException, JsonProcessingException, CloudbreakOrchestratorFailedException {
        MockitoAnnotations.initMocks(this);
        when(hostOrchestratorResolver.get(anyString())).thenReturn(hostOrchestrator);
        when(gatewayConfigService.getGatewayConfig(any(Stack.class), any(InstanceMetaData.class), anyBoolean())).thenReturn(gatewayConfig);
        when(cloudbreakMessagesService.getMessage(anyString(), anyCollection())).thenReturn("message");

        InstanceMetadataUpdater.Package packageByName = new InstanceMetadataUpdater.Package();
        this.packageByName = "packageByName";
        packageByName.setName(this.packageByName);
        packageByName.setPkgName(Collections.singletonList("packageByName"));
        InstanceMetadataUpdater.Package packageByCmd = new InstanceMetadataUpdater.Package();
        this.packageByCmd = "packageByCmd";
        packageByCmd.setName(this.packageByCmd);
        packageByCmd.setCommand("packageByCmd");

        stack = new Stack();
        stack.setId(1L);
        stack.setCluster(new Cluster());
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("salt");
        stack.setOrchestrator(orchestrator);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        Image image = new Image("imagename", null, "os", "ostype", "catalogurl",
                "catalogname", "iamgeid", Map.of(packageByName.getName(), "1", packageByCmd.getName(), "1"));
        instanceMetaData.setImage(new Json(image));
        instanceMetaData.setInstanceId("instanceId");
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup);
        stack.setInstanceGroups(instanceGroups);

        when(hostOrchestrator.runCommandOnAllHosts(any(GatewayConfig.class), anyString())).thenReturn(Collections.singletonMap("hostByCmd", "1"));
        when(hostOrchestrator.getPackageVersionsFromAllHosts(any(GatewayConfig.class), any()))
                .thenReturn(Collections.singletonMap("instanceId", Collections.singletonMap("packageByName", "1")));

        underTest.setPackages(Lists.newArrayList(packageByName, packageByCmd));
    }

    @Test
    public void updatePackageVersionsOnAllInstances() throws Exception {
        underTest.updatePackageVersionsOnAllInstances(stack);

        verify(cloudbreakEventService, times(0)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
    }

    @Test
    public void updatePackageVersionsOnAllInstancesInstanceMissingPackageversion() throws Exception {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.CORE);
        Image image = new Image("imagename", null, "os", "ostype", "catalogurl",
                "catalogname", "iamgeid", Collections.emptyMap());
        instanceMetaData.setImage(new Json(image));
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));
        instanceGroups.add(instanceGroup);

        underTest.updatePackageVersionsOnAllInstances(stack);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        verify(cloudbreakMessagesService, times(1))
                .getMessage(eq(InstanceMetadataUpdater.Msg.PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING.code()), anyCollection());
    }

    @Test
    public void updatePackageVersionsOnAllInstancesDifferentVersion() throws Exception {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.CORE);
        Image image = new Image("imagename", null, "os", "ostype", "catalogurl",
                "catalogname", "iamgeid", Map.of(packageByName, "2", packageByCmd, "2"));
        instanceMetaData.setImage(new Json(image));
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));
        instanceGroups.add(instanceGroup);

        underTest.updatePackageVersionsOnAllInstances(stack);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        verify(cloudbreakMessagesService, times(1))
                .getMessage(eq(InstanceMetadataUpdater.Msg.PACKAGES_ON_INSTANCES_ARE_DIFFERENT.code()), anyCollection());
    }

    @Test
    public void collectPackagesWithMultipleVersions() {
    }

    @Test
    public void collectInstancesWithMissingPackageVersions() {
    }
}