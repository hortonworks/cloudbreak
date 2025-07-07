package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestUtil;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestValidator;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

public class SdxCustomInstanceResizeTests extends PreconditionSdxE2ETest {
    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxResizeTestUtil sdxResizeTestUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster (CUSTOM shape, single-az)",
            when = "resize is called on the SDX cluster with custom instance type and custom disk size on master instance group",
            then = "SDX resize should be successful, the new cluster running with custom instances from previous SDX cluster"
    )
    public void testSdxCustomInstanceResize(TestContext testContext) {
        String sdxKey = resourcePropertyProvider().getName();
        SdxResizeTestValidator validator = new SdxResizeTestValidator(SdxClusterShape.ENTERPRISE);
        String runtimeVersion = commonClusterManagerProperties.getRuntimeVersion();
        String instanceType = switch (testContext.getCloudPlatform().name().toLowerCase()) {
            case "aws" -> "m5.4xlarge";
            case "azure" -> "Standard_D8s_v3";
            case "gcp" -> "n2-standard-8";
            default -> throw new TestFailException("Custom instanceType has no value for cloud platform:" + testContext.getCloudPlatform().name());
        };

        sdxResizeTestUtil
            .givenProvisionEnvironmentAndDatalake(testContext, sdxKey, runtimeVersion, SdxClusterShape.CUSTOM, validator)
            .then((tc, testDto, client) -> {
                SdxClusterResizeRequest sdxClusterResizeRequest = testDto.getSdxResizeRequest();
                sdxClusterResizeRequest.setClusterShape(SdxClusterShape.ENTERPRISE);
                sdxClusterResizeRequest.setSkipValidation(true);

                SdxInstanceGroupRequest sdxInstanceGroupRequest = new SdxInstanceGroupRequest();
                sdxInstanceGroupRequest.setName("master");
                sdxInstanceGroupRequest.setInstanceType(instanceType);
                SdxInstanceGroupDiskRequest sdxInstanceGroupDiskRequest = new SdxInstanceGroupDiskRequest();
                sdxInstanceGroupDiskRequest.setName("master");
                sdxInstanceGroupDiskRequest.setInstanceDiskSize(300);

                sdxClusterResizeRequest.setCustomInstanceGroups(List.of(sdxInstanceGroupRequest));
                sdxClusterResizeRequest.setCustomInstanceGroupDiskSize(List.of(sdxInstanceGroupDiskRequest));
                validator.setExpectedCustomInstanceGroups(sdxClusterResizeRequest.getCustomInstanceGroups());
                validator.setExpectedSdxInstanceGroupDiskRequest(sdxClusterResizeRequest.getCustomInstanceGroupDiskSize());
                validator.preValidateCustomInstanceGroups(testDto);
                return testDto;
            })
            .when(sdxTestClient.resize(), key(sdxKey))
            .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
            .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
            .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
            .awaitForHealthyInstances()
            .then((tc, dto, client) -> validator.validateResizedCluster(dto))
            .validate();
    }
}
