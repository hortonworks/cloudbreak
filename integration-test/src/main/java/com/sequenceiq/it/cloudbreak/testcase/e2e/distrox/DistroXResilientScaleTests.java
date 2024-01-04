package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.assertion.distrox.DistroxScaleThresholdAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class DistroXResilientScaleTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXResilientScaleTests.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        // Along with "CB-12902 Add AwsNativeSetup" was introduced an extra AWS autoscailing validation:
        // "Not all the existing instances are in [Started] state, upscale is not possible!"
        // by /cloud-aws-cloudformation/src/main/java/com/sequenceiq/cloudbreak/cloud/aws/AwsCloudFormationSetup.java#L65-L69
        // So this test is not supported at AWS right now.
        assertNotSupportedCloudPlatform(CloudPlatform.AWS);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, description = "Resilient Scaling: " +
            "UseCase4: " +
            "- Start upscale on running cluster with percentage option with at least option " +
            "- Provider only gives back above threshold " +
            "- Upscale should complete ")
    @Description(
            given = "there is a running default Distrox cluster",
            when = "cluster has been scaled up at least 90% of the required node count",
                and = "cluster has been scaled up again while removing 10% of the instance from the scaled host group",
            then = "cluster can be resiliently scaled up at least the required node count threshold")
    public void testPercentageResilientScaleDistrox(TestContext testContext, ITestContext iTestContext) {
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());

        testContext
                .given(DistroXTestDto.class)
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget(), params.getAdjustmentType(), params.getThreshold()))
                .awaitForFlow()
                .then(new DistroxScaleThresholdAssertions(params.getHostGroup(), params.getScaleUpTarget(), params.getThreshold()))
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleDownTarget()))
                .awaitForFlow()
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget(), params.getAdjustmentType(), params.getThreshold()))
                .then((tc, testDto, client) -> removeInstanceWhileScalingGroup(tc, testDto, client, params))
                .awaitForFlow()
                .when(distroXTestClient.get())
                .then(new DistroxScaleThresholdAssertions(params.getHostGroup(), params.getScaleUpTarget(), params.getThreshold()))
                .validate();
    }

    private long getDeletionLimit(DistroXScaleTestParameters params) {
        float scalingPercentage = (float) params.getThreshold() / 100;
        float scalingThreshold = params.getScaleUpTarget() * scalingPercentage;

        return params.getScaleUpTarget() - (long) scalingThreshold;
    }

    private DistroXTestDto removeInstanceWhileScalingGroup(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client,
            DistroXScaleTestParameters params) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<String> instancesToDelete = distroxUtil.getInstanceIds(testDto, client, params.getHostGroup()).stream()
                .limit(getDeletionLimit(params)).collect(Collectors.toList());
        if (instancesToDelete.isEmpty()) {
            throw new TestFailException(String.format("At least 1 instance needed from '%s' group to delete!", params.getHostGroup()));
        }
        cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete);
        return testDto;
    }
}
