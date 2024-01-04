package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.audit.DatahubAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class DistroXClusterStopStartTest extends AbstractMockTest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DatahubAuditGrpcServiceAssertion auditGrpcServiceAssertion;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster",
            when = "a scale, start stop called many times",
            then = "the cluster should be available")
    public void testCreateNewRegularDistroXClusterScaleStartStop(MockedTestContext testContext, ITestContext testNgContext) {
        DistroXStartStopTestParameters params = new DistroXStartStopTestParameters(testNgContext.getCurrentXmlTest().getAllParameters());

        if (params.getTimes() < 1) {
            throw new TestFailException("Scaling Configuration ERROR: At least 1 round of scaling should be set" +
                    " with the [DistroXStartStopTestParameters.times] parameter!");
        }

        IntStream.range(1, params.getTimes()).forEach(i -> testContext.given(DistroXTestDto.class)
                .when(distroXTestClient.stop())
                .awaitForFlow()
                .await(STACK_STOPPED)
                .then(auditGrpcServiceAssertion::stop)
                .when(distroXTestClient.start())
                .awaitForFlow()
                .await(STACK_AVAILABLE)
                .then(auditGrpcServiceAssertion::start)
                .when(distroXTestClient.scale(params.getHostgroup(), params.getStep() + i))
                .awaitForFlow()
                .await(STACK_AVAILABLE)
                .validate());
    }
}
