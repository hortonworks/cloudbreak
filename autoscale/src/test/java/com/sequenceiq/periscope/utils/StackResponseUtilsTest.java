package com.sequenceiq.periscope.utils;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.periscope.utils.MockStackResponseGenerator.getMockStackResponseWithDependentHostGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
public class StackResponseUtilsTest {

    @InjectMocks
    private StackResponseUtils underTest;

    @Test
    public void testGetCloudInstanceIdsForHostGroup() {
        String hostGroup = "compute";

        Map<String, String> instanceIdsForHostGroups = underTest
                .getCloudInstanceIdsForHostGroup(getMockStackV4Response(hostGroup, 0), hostGroup);

        assertEquals(instanceIdsForHostGroups.size(), 3, "Retrieved Instance Ids size should match");
        assertEquals(instanceIdsForHostGroups.get("test_fqdn1"), "test_instanceid_compute1", "Retrieved Instance Id should match");
        assertEquals(instanceIdsForHostGroups.get("test_fqdn2"), "test_instanceid_compute2", "Retrieved Instance Id should match");
        assertEquals(instanceIdsForHostGroups.get("test_fqdn3"), "test_instanceid_compute3", "Retrieved Instance Id should match");
    }

    @Test
    public void testGetCloudInstanceIdsForHostGroupWithUnhealthyInstances() {
        String hostGroup = "compute";

        int servicesHealthyHostGroupSize = underTest.
                getCloudInstanceIdsWithServicesHealthyForHostGroup(getMockStackV4Response(hostGroup, 2), hostGroup).size();

        assertEquals(1, servicesHealthyHostGroupSize, "Retrieved healthy host group size should match");
    }

    @Test
    public void testGetStoppedInstanceCountInHostGroup() {
        String hostGroup = "compute";
        Integer runningHostGroupCount = 1;
        Integer stoppedHostGroupCount = 3;

        Integer stoppedInstanceCount = underTest.getStoppedCloudInstanceIdsInHostGroup(getMockStackV4ResponseForStopStart(hostGroup,
                runningHostGroupCount, stoppedHostGroupCount), hostGroup).size();
        assertEquals(Integer.valueOf(3), stoppedInstanceCount, "Stopped instance count should match");
    }

    @Test
    public void testGetNodeCountForHostGroup() {
        String hostGroup = "compute";

        Integer nodeCountForHostGroup = underTest
                .getNodeCountForHostGroup(getMockStackV4Response(hostGroup, 0), hostGroup);
        assertEquals(Integer.valueOf(3), nodeCountForHostGroup, "Retrieved HostGroup Instance Count should match.");
    }

    @Test
    public void testGetRoleConfigNameForHostGroup() throws Exception {
        validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                "hivecompute", "yarn-NODEMANAGER-hivecompute");

        validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                "worker", "yarn-NODEMANAGER-worker");

        validateGetRoleConfigNameForHostGroup("YARN", "NODEMANAGER",
                "abinitiocompute", "yarn-NODEMANAGER-abinitiocompute");
    }

    @Test
    public void testGetRoleTypesOnHostGroup() throws Exception {
        validateGetRoleTypesOnHostGroup("compute",
                Set.of("NODEMANAGER", "GATEWAY"));

        validateGetRoleTypesOnHostGroup("worker",
                Set.of("NODEMANAGER", "GATEWAY", "DATANODE"));

        validateGetRoleTypesOnHostGroup("compute1", Set.of());
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

    @Test
    public void testPrimaryGatewayHealthy() {
        String policyHostGroup = "compute";
        StackV4Response mockStackResponse = getMockStackV4Response(policyHostGroup, 0);

        assertTrue(underTest.primaryGatewayHealthy(mockStackResponse));
    }

    @Test
    public void testPrimaryGatewayUnhealthy() {
        StackV4Response mockStackResponse = getMockStackResponseWithDependentHostGroup(AVAILABLE, Set.of("master"), SERVICES_UNHEALTHY);

        assertFalse(underTest.primaryGatewayHealthy(mockStackResponse));
    }

    @Test
    public void testGetUnhealthyDependentHostsMasterUnhealthy() {
        String policyHostGroup = "compute";
        StackV4Response mockStackResponse = getMockStackResponseWithDependentHostGroup(AVAILABLE, Set.of("master"), SERVICES_RUNNING);
        DependentHostGroupsV4Response mockDependentHostGroupsResponse = getDependentHostGroupsResponse(policyHostGroup,
                "master", "gateway");

        Set<String> result = underTest.getUnhealthyDependentHosts(mockStackResponse, mockDependentHostGroupsResponse, policyHostGroup);
        assertThat(result).hasSameElementsAs(Set.of("fqdn-master"));
    }

    @Test
    public void testGetUnhealthyDependentHostsNoneUnhealthy() {
        String policyHostGroup = "compute";
        StackV4Response mockStackResponse = getMockStackResponseWithDependentHostGroup(AVAILABLE, Set.of("master", "gateway"), SERVICES_HEALTHY);
        DependentHostGroupsV4Response mockDependentHostGroupsResponse = getDependentHostGroupsResponse(policyHostGroup,
                "master", "gateway");

        Set<String> result = underTest.getUnhealthyDependentHosts(mockStackResponse, mockDependentHostGroupsResponse, policyHostGroup);
        assertThat(result).isEmpty();
    }

    private void validateGetRoleConfigNameForHostGroup(String testService, String testRole,
            String testHostGroup, String expectedRoleConfigName) throws Exception {
        StackV4Response mockStackResponse = mock(StackV4Response.class);
        ClusterV4Response mockCluster = mock(ClusterV4Response.class);

        when(mockStackResponse.getCluster()).thenReturn(mockCluster);
        when(mockCluster.getExtendedBlueprintText()).thenReturn(getExtendedBP());
        String hostGroupRolename = underTest.getRoleConfigNameForHostGroup(mockStackResponse, testHostGroup, testService, testRole);
        assertEquals(expectedRoleConfigName, hostGroupRolename, "RoleConfigName in template should match for HostGroup");
    }

    private void validateGetRoleTypesOnHostGroup(String testHostGroup, Set<String> expectedServices) throws Exception {
        StackV4Response mockStackResponse = mock(StackV4Response.class);
        ClusterV4Response mockCluster = mock(ClusterV4Response.class);
        BlueprintV4Response mockBluePrint = mock(BlueprintV4Response.class);

        Set<String> servicesOnHostGroup = underTest.getRoleTypesOnHostGroup(getTestBP(), testHostGroup);
        assertEquals(expectedServices, servicesOnHostGroup, "RoleConfigName in template should match for HostGroup");
    }

    private StackV4Response getMockStackV4Response(String hostGroup, int unhealthyInstancesCount) {
        return MockStackResponseGenerator
                .getMockStackV4Response("test-crn", hostGroup, "test_fqdn", 3, unhealthyInstancesCount);
    }

    private StackV4Response getMockStackV4ResponseForStopStart(String hostGroup, int runningHostGroupCount, int stoppedHostGroupCount) {
        return MockStackResponseGenerator
                .getMockStackV4ResponseWithStoppedAndRunningNodes("test-crn", hostGroup, "test-fqdn", runningHostGroupCount, stoppedHostGroupCount);
    }

    private DependentHostGroupsV4Response getDependentHostGroupsResponse(String policyHostGroup, String... dependentHostGroups) {
        DependentHostGroupsV4Response response = new DependentHostGroupsV4Response();
        Map<String, Set<String>> dependentHostGroupsMap = Map.of(policyHostGroup, Set.of(dependentHostGroups));
        response.setDependentHostGroups(dependentHostGroupsMap);
        return response;
    }

    private String getTestBP() throws IOException {
        return FileReaderUtils.readFileFromClasspath("/dataengineering-test.json");
    }

    private String getExtendedBP() throws IOException {
        return FileReaderUtils.readFileFromClasspath("/customdataengineering-test.json");
    }
}
