package com.sequenceiq.it.cloudbreak.testcase.e2e.openstack;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.openstack.OpenStackProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

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
}
