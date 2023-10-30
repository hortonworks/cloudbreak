package com.sequenceiq.cloudbreak.service.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class KnoxGroupDeterminerTest {

    @Mock
    private Blueprint blueprint;

    @InjectMocks
    private KnoxGroupDeterminer underTest;

    @Test
    void testGetKnoxGatewayThrowsErrorWhenKnoxExplicitlyDefined() {
        Set<String> groups = Set.of("master");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setName("tenant");
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setGroupName("gateway");
        instanceGroup1.setInstanceGroupType(InstanceGroupType.GATEWAY);
        stack.setInstanceGroups(Set.of(instanceGroup, instanceGroup1));
        stack.setCluster(cluster);

        when(blueprint.getBlueprintJsonText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(blueprint.getName()).thenReturn("dummy");
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getKnoxGatewayGroupNames(stack));
        assertEquals(exception.getMessage(), "KNOX can only be installed on instance group where type is GATEWAY. As per the template " +
                "dummy" + " KNOX_GATEWAY role config is present in groups " + groups +
                " while the GATEWAY nodeType is available for instance group " + Set.of("gateway"));
    }

    @Test
    void testGetKnoxGatewayWhenMoreGatewayGroupPresentedThenShouldLogBlueprintName() {
        Set<String> groups = Set.of("master");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setName("tenant");
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setGroupName("gateway");
        instanceGroup1.setInstanceGroupType(InstanceGroupType.GATEWAY);
        stack.setInstanceGroups(Set.of(instanceGroup, instanceGroup1));
        stack.setCluster(cluster);

        when(blueprint.getBlueprintJsonText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(blueprint.getName()).thenReturn("dummy");
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getKnoxGatewayGroupNames(stack));
        assertEquals(exception.getMessage(), "KNOX can only be installed on instance group where type is GATEWAY. As per the template " +
                "dummy" + " KNOX_GATEWAY role config is present in groups " + groups +
                " while the GATEWAY nodeType is available for instance group " + Set.of("gateway"));
    }

    @Test
    void testGetKnoxGatewayWhenKnoxExplicitlyDefined() {
        Set<String> groups = Set.of("master");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        workspace.setName("tenant");
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        stack.setInstanceGroups(Set.of(instanceGroup));
        stack.setCluster(cluster);

        when(blueprint.getBlueprintJsonText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        Set<String> selectedGroups = underTest.getKnoxGatewayGroupNames(stack);
        assertEquals(groups, selectedGroups);
    }

    @Test
    void testGetKnoxGatewayWhenKnoxImplicitlyDefinedByGatewayGroup() {
        Set<String> groups = Set.of("gateway");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));
        Workspace workspace = new Workspace();
        workspace.setName("tenant");
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        when(blueprint.getBlueprintJsonText()).thenReturn("{}");

        Set<String> selectedGroups = underTest.getKnoxGatewayGroupNames(stack);
        assertEquals(groups, selectedGroups);
    }

    @Test
    void testGetKnoxGatewayWhenNoGateway() {
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        Workspace workspace = new Workspace();
        workspace.setName("tenant");
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        when(blueprint.getBlueprintJsonText()).thenReturn("{}");

        Set<String> selectedGroups = underTest.getKnoxGatewayGroupNames(stack);
        assertThat(selectedGroups).isEmpty();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
