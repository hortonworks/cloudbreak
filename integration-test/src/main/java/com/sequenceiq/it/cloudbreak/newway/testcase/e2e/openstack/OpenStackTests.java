package com.sequenceiq.it.cloudbreak.newway.testcase.e2e.openstack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.openstack.OpenStackProperties;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.SparklessTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;

public class OpenStackTests extends AbstractE2ETest {

    private static final String OPEN_STACK_HDF_TESTS_DATA_PROVIDER = "openStackHdfTestsDataProvider";

    private static final String OPEN_STACK_HDP_TESTS_DATA_PROVIDER = "openStackHdpTestsDataProvider";

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private OpenStackProperties openStackProperties;

    @Override
    protected void setupTest(TestContext testContext) {
    }

    @Test(dataProvider = OPEN_STACK_HDP_TESTS_DATA_PROVIDER)
    public void testOpenStackWithDefaulkHDPBlueprintAndPrewarmedImagesWithoutGateWaySetup(
            TestContext testContext,
            String blueprintName,
            List<String> groups,
            String scaleGroup,
            @Description TestCaseDescription description) {

        testContext
                .given(ClusterTestDto.class)
                .withBlueprintName(blueprintName)
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

    @Test(dataProvider = OPEN_STACK_HDF_TESTS_DATA_PROVIDER)
    public void testOpenStackWithDefaulkHDFBlueprintAndPrewarmedImagesWithoutGateWaySetup(
            TestContext testContext,
            String blueprintName,
            List<String> groups,
            String scaleGroup,
            @Description TestCaseDescription description) {

        testContext
                .given(ClusterTestDto.class)
                .withBlueprintName(blueprintName)
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

    @DataProvider(name = OPEN_STACK_HDP_TESTS_DATA_PROVIDER)
    public Object[][] openStackHdpTestsDataProvider() {
        SparklessTestContext testContext = getBean(SparklessTestContext.class);
        super.setupTest(testContext);
        BlueprintTestDto blueprint = testContext
                .given(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4());
        blueprint.validate();
        testContext.cleanupTestContext();
        List<BlueprintV4ViewResponse> viewResponses = blueprint.getViewResponses()
                .stream()
                .filter(e -> e.getStatus().equals(ResourceStatus.DEFAULT))
                .filter(e -> isHdp(e) && notASharedServiceBlueprint(e))
                .collect(Collectors.toList());

        Object[][] data = new Object[viewResponses.size()][5];
        for (int i = 0; i < viewResponses.size(); i++) {
            SparklessTestContext tc = getBean(SparklessTestContext.class);
            super.setupTest(tc);
            data[i][0] = tc;
            data[i][1] = viewResponses.get(i).getName();
            data[i][2] = getHdpGroups();
            data[i][3] = getHdpScaleGroup();
            data[i][4] = new TestCaseDescription.TestCaseDescriptionBuilder()
                    .given("there is an available cloudbreak")
                    .when("a stack create request with "
                            + viewResponses.get(i).getName() + " blueprint is sent AND scale AND stop AND start requests are sent")
                    .then("all stack operation should succeed and the stack should be deletable");
        }
        return data;
    }

    @DataProvider(name = OPEN_STACK_HDF_TESTS_DATA_PROVIDER)
    public Object[][] openStackHdfTestsDataProvider() {
        Object[][] data = new Object[getHdfBlueprint().size()][5];
        for (int i = 0; i < getHdfBlueprint().size(); i++) {
            SparklessTestContext testContext = getBean(SparklessTestContext.class);
            super.setupTest(testContext);
            data[i][0] = testContext;
            data[i][1] = getHdfBlueprint().get(i);
            data[i][2] = getHdfGroups();
            data[i][3] = getHdfScaleGroup();
            data[i][4] = new TestCaseDescription.TestCaseDescriptionBuilder()
                    .given("there is an available cloudbreak")
                    .when("a stack create request with "
                            + getHdfBlueprint().get(i) + " blueprint is sent AND scale AND stop AND start requests are sent")
                    .then("all stack operation should succeed and the stack should be deletable");
        }
        if (isHdfEnabled()) {
            return data;
        }
        return new Object[0][5];
    }

    private List<InstanceGroupTestDto> getInstanceGroupTestDtos(TestContext testContext, List<String> groups) {
        List<InstanceGroupTestDto> instanceGroupTestDtos = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            HostGroupType hostGroupType = HostGroupType.getByName(groups.get(i));
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

    private boolean notASharedServiceBlueprint(BlueprintV4ViewResponse e) {
        return !e.getTags().keySet().contains("shared_services_ready");
    }

    private boolean isHdp(BlueprintV4ViewResponse e) {
        return e.getStackType().equals("HDP") && e.getStackVersion().equals(getHdpVersion()) && isHdpEnabled();
    }

    private String getHdpVersion() {
        return openStackProperties.getPrewarmed().getHdp().getVersion();
    }

    private String getHdpScaleGroup() {
        return openStackProperties.getPrewarmed().getHdp().getScaleGroup();
    }

    private List<String> getHdpGroups() {
        return openStackProperties.getPrewarmed().getHdp().getHostGroups();
    }

    private boolean isHdpEnabled() {
        return openStackProperties.getPrewarmed().getHdp().getEnabled();
    }

    private List<String> getHdfBlueprint() {
        return openStackProperties.getPrewarmed().getHdf().getBlueprintNames();
    }

    private String getHdfScaleGroup() {
        return openStackProperties.getPrewarmed().getHdf().getScaleGroup();
    }

    private List<String> getHdfGroups() {
        return openStackProperties.getPrewarmed().getHdf().getHostGroups();
    }

    private boolean isHdfEnabled() {
        return openStackProperties.getPrewarmed().getHdf().getEnabled();
    }

}
