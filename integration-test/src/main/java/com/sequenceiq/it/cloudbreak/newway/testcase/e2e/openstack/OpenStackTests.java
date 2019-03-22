package com.sequenceiq.it.cloudbreak.newway.testcase.e2e.openstack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.it.cloudbreak.newway.client.ClusterDefinitionTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.openstack.OpenStackParameters;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.SparklessTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;

public class OpenStackTests extends AbstractE2ETest {

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private ClusterDefinitionTestClient clusterDefinitionTestClient;

    @Test(dataProvider = "openStackHdpTestsDataProvider")
    public void testOpenStackWithDefaulkHDPClusterDefinitionAndPrewarmedImagesWithoutGateWaySetup(
            TestContext testContext,
            String clusterDefinitionName,
            String[] groups,
            String scaleGroup,
            @Description TestCaseDescription description) {

        testContext
                .given(ClusterTestDto.class)
                .withClusterDefinitionName(clusterDefinitionName)
                .given(StackTestDto.class)
                .withInstanceGroupsEntity(getInstanceGroupTestDtos(testContext, groups))
                .withCluster(ClusterTestDto.class.getSimpleName())
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.scalePostV4()
                        .withGroup(scaleGroup)
                        .withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .when(stackTestClient.scalePostV4()
                        .withGroup(scaleGroup)
                        .withDesiredCount(1))
                .await(STACK_AVAILABLE)
                .when(stackTestClient.stopV4())
                .await(STACK_STOPPED)
                .when(stackTestClient.startV4())
                .await(STACK_AVAILABLE)
                .then((tc, testDto, cc) -> stackTestClient.deleteV4().action(tc, testDto, cc))
                .validate();
    }

    @DataProvider(name = "openStackHdpTestsDataProvider")
    public Object[][] openStackHdpTestsDataProvider() {
        SparklessTestContext testContext = getBean(SparklessTestContext.class);
        minimalSetupForClusterCreation(testContext);
        ClusterDefinitionTestDto select = testContext
                .given(ClusterDefinitionTestDto.class)
                .when(clusterDefinitionTestClient.listV4());
        List<ClusterDefinitionV4ViewResponse> viewResponses = select.getViewResponses()
                .stream()
                .filter(e -> e.getStatus().equals(ResourceStatus.DEFAULT))
                .filter(e -> isHdp(e) && notASharedServiceClusterDefinition(e))
                .collect(Collectors.toList());

        Object[][] data = new Object[viewResponses.size()][5];
        for (int i = 0; i < viewResponses.size(); i++) {
            data[i][0] = testContext;
            data[i][1] = viewResponses.get(i).getName();
            data[i][2] = getHdpGroups();
            data[i][3] = getHdpScaleGroup();
            data[i][4] = new TestCaseDescription.TestCaseDescriptionBuilder()
                    .given("there is an available cloudbreak")
                    .when("a stack create request with "
                            + viewResponses.get(i).getName() + " cluster definition is sent AND scale AND stop AND start requests are sent")
                    .then("all stack operation should succeed and the stack should be deletable");
        }
        return data;
    }

    @Test(dataProvider = "openStackHdfTestsDataProvider")
    public void testOpenStackWithDefaulkHDFClusterDefinitionAndPrewarmedImagesWithoutGateWaySetup(
            TestContext testContext,
            String clusterDefinitionName,
            String[] groups,
            String scaleGroup,
            @Description TestCaseDescription description) {

        testContext
                .given(ClusterTestDto.class)
                .withClusterDefinitionName(clusterDefinitionName)
                .given(StackTestDto.class)
                .withInstanceGroupsEntity(getInstanceGroupTestDtos(testContext, groups))
                .withCluster(ClusterTestDto.class.getSimpleName())
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.scalePostV4()
                        .withGroup(scaleGroup)
                        .withDesiredCount(2))
                .await(STACK_AVAILABLE)
                .when(stackTestClient.scalePostV4()
                        .withGroup(scaleGroup)
                        .withDesiredCount(1))
                .await(STACK_AVAILABLE)
                .when(stackTestClient.stopV4())
                .await(STACK_STOPPED)
                .when(stackTestClient.startV4())
                .await(STACK_AVAILABLE)
                .then((tc, testDto, cc) -> stackTestClient.deleteV4().action(tc, testDto, cc))
                .validate();
    }

    @DataProvider(name = "openStackHdfTestsDataProvider")
    public Object[][] openStackHdfTestsDataProvider() {
        SparklessTestContext testContext = getBean(SparklessTestContext.class);
        minimalSetupForClusterCreation(testContext);

        Object[][] data = new Object[getHdfClusterDefinition().length][5];
        for (int i = 0; i < getHdfClusterDefinition().length; i++) {
            data[i][0] = testContext;
            data[i][1] = getHdfClusterDefinition()[i];
            data[i][2] = getHdfGroups();
            data[i][3] = getHdfScaleGroup();
            data[i][4] = new TestCaseDescription.TestCaseDescriptionBuilder()
                    .given("there is an available cloudbreak")
                    .when("a stack create request with "
                            + getHdfClusterDefinition()[i] + " cluster definition is sent AND scale AND stop AND start requests are sent")
                    .then("all stack operation should succeed and the stack should be deletable");
        }
        if (isHdfEnabled()) {
            return data;
        }
        return new Object[0][5];
    }

    private List<InstanceGroupTestDto> getInstanceGroupTestDtos(TestContext testContext, String[] groups) {
        List<InstanceGroupTestDto> instanceGroupTestDtos = new ArrayList<>();
        for (int i = 0; i < groups.length; i++) {
            HostGroupType hostGroupType = HostGroupType.getByName(groups[i]);
            InstanceGroupTestDto instanceGroupTestDto = testContext
                    .given(InstanceGroupTestDto.class)
                    .withHostGroup(
                            testContext,
                            hostGroupType,
                            HostGroupType.WORKER.getName().equals(hostGroupType.getName()) ? 3 : 1);
            instanceGroupTestDtos.add(instanceGroupTestDto);
        }
        return instanceGroupTestDtos;
    }

    private boolean notASharedServiceClusterDefinition(ClusterDefinitionV4ViewResponse e) {
        return !e.getTags().keySet().contains("shared_services_ready");
    }

    private boolean isHdp(ClusterDefinitionV4ViewResponse e) {
        return e.getStackType().equals("HDP") && e.getStackVersion().equals(getHdpVersion()) && isHdpEnabled();
    }

    @AfterMethod(alwaysRun = true)
    public void teardown(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContext();
    }

    public String getHdpVersion() {
        return getTestParameter().getWithDefault(
                OpenStackParameters.PreWarmedTest.HDP_VERSION,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDP_VERSION);
    }

    public String getHdpScaleGroup() {
        return getTestParameter().getWithDefault(
                OpenStackParameters.PreWarmedTest.HDP_SCALE_GROUP,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDP_SCALE_GROUP);
    }

    public String[] getHdpGroups() {
        return getTestParameter().getWithDefault(
                OpenStackParameters.PreWarmedTest.HDP_GROUPS,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDP_GROUPS).split(",");
    }

    public boolean isHdpEnabled() {
        return getTestParameter().getWithBooleanDefault(
                OpenStackParameters.PreWarmedTest.HDP_ENABLED,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDP_ENABLED);
    }

    public String[] getHdfClusterDefinition() {
        return getTestParameter().getWithDefault(
                OpenStackParameters.PreWarmedTest.HDF_CLUSTER_DEFINITION,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDF_CLUSTER_DEFINITION).split(";");
    }

    public String getHdfScaleGroup() {
        return getTestParameter().getWithDefault(
                OpenStackParameters.PreWarmedTest.HDF_SCALE_GROUP,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDF_SCALE_GROUP);
    }

    public String[] getHdfGroups() {
        return getTestParameter().getWithDefault(
                OpenStackParameters.PreWarmedTest.HDF_GROUPS,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDF_GROUPS).split(",");
    }

    public boolean isHdfEnabled() {
        return getTestParameter().getWithBooleanDefault(
                OpenStackParameters.PreWarmedTest.HDF_ENABLED,
                OpenStackParameters.PreWarmedTest.DEFAULT_HDF_ENABLED);
    }
}
