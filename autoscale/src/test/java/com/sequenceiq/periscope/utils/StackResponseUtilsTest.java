package com.sequenceiq.periscope.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class StackResponseUtilsTest {

    private StackResponseUtils underTest = new StackResponseUtils();

    @Test
    public void testGetCloudInstanceIdsForHostGroup() {
        String hostGroup = "compute";

        Map<String, String> instanceIdsForHostGroups = underTest
                .getCloudInstanceIdsForHostGroup(getMockStackV4Response(hostGroup), hostGroup);

        assertEquals("Retrieved Instance Ids size should match", instanceIdsForHostGroups.size(), 3);
        assertEquals("Retrieved Instance Id should match",
                instanceIdsForHostGroups.get("test_fqdn1"), "test_instanceid1");
        assertEquals("Retrieved Instance Id should match",
                instanceIdsForHostGroups.get("test_fqdn2"), "test_instanceid2");
        assertEquals("Retrieved Instance Id should match",
                instanceIdsForHostGroups.get("test_fqdn3"), "test_instanceid3");
    }

    @Test
    public void testGetNodeCountForHostGroup() {
        String hostGroup = "compute";

        Integer nodeCountForHostGroup = underTest
                .getNodeCountForHostGroup(getMockStackV4Response(hostGroup), hostGroup);
        assertEquals("Retrieved HostGroup Instance Count should match.", Integer.valueOf(3), nodeCountForHostGroup);
    }

    @Test
    public void testGetRoleConfigNameForHostGroup() throws Exception {
        validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                "compute", "yarn-NODEMANAGER-COMPUTE");

        validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                "worker", "yarn-NODEMANAGER-WORKER");

        validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                "randomHostGroup", "random-NODEMANAGER-BASE");
    }

    @Test
    public void testGetRoleConfigNameForHostGroupWhenInvalidHostGroup() {
        Exception exception = assertThrows(Exception.class, () ->
                validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                "compute1", ""));
        assertTrue(exception.getMessage().contains("Unable to retrieve RoleConfigGroupRefName for Service 'YARN'"));
        assertTrue(exception.getMessage().contains("compute1"));
    }

    @Test
    public void testGetRoleConfigNameForHostGroupWhenHostGroupWithoutNodeManager() {
        Exception exception = assertThrows(Exception.class, () ->
                validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                        "gateway", ""));
        assertTrue(exception.getMessage().contains("Unable to retrieve RoleConfigGroupRefName for Service 'YARN'"));
        assertTrue(exception.getMessage().contains("gateway"));
    }

    private void validateGetRoleConfigNameForHostGroup(String testService, String testRole,
            String testHostGroup, String expectedRoleConfigName) throws Exception {
        StackV4Response mockStackResponse = mock(StackV4Response.class);
        ClusterV4Response mockCluster = mock(ClusterV4Response.class);
        BlueprintV4Response mockBluePrint = mock(BlueprintV4Response.class);

        when(mockStackResponse.getCluster()).thenReturn(mockCluster);
        when(mockCluster.getBlueprint()).thenReturn(mockBluePrint);
        when(mockBluePrint.getBlueprint()).thenReturn(getTestBP());
        String hostGroupRolename = underTest.getRoleConfigNameForHostGroup(mockStackResponse, testHostGroup, testService, testRole);
        assertEquals("RoleConfigName in template should match for HostGroup", expectedRoleConfigName, hostGroupRolename);
    }

    private StackV4Response getMockStackV4Response(String hostGroup) {
        Set hostGroupInstanceType = Set.of("compute1", "master1", "worker1", hostGroup);

        InstanceMetaDataV4Response metadata1 = new InstanceMetaDataV4Response();
        metadata1.setDiscoveryFQDN("test_fqdn1");
        metadata1.setInstanceId("test_instanceid1");

        InstanceMetaDataV4Response metadata2 = new InstanceMetaDataV4Response();
        metadata2.setDiscoveryFQDN("test_fqdn2");
        metadata2.setInstanceId("test_instanceid2");

        InstanceMetaDataV4Response metadata3 = new InstanceMetaDataV4Response();
        metadata3.setDiscoveryFQDN("test_fqdn3");
        metadata3.setInstanceId("test_instanceid3");

        return MockStackResponseGenerator.getMockStackV4Response("test-crn",
                hostGroupInstanceType,
                Set.of(metadata1, metadata2, metadata3));
    }

    private String getTestBP() throws IOException {
        return FileReaderUtils.readFileFromClasspath("/dataengineering-test.json");
    }
}
