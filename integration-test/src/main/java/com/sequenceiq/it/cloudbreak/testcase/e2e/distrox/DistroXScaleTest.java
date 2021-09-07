package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.stream.IntStream;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class DistroXScaleTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        initiateEnvironmentCreation(testContext);
        initiateDatalakeCreation(testContext);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        waitForDatalakeCreation(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent",
            then = "DistroX cluster can be scaled up and down with higher node count")
    public void testCreateAndScaleDistroX(TestContext testContext, ITestContext iTestContext) {
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());
        testContext.given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE);
        IntStream.range(0, params.getTimes()).forEach(i -> {
            testContext.given(DistroXTestDto.class)
                    .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget()))
                    .await(STACK_AVAILABLE)
                    .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleDownTarget()))
                    .await(STACK_AVAILABLE);
        });
        testContext.given(DistroXTestDto.class).validate();
    }

}
