package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.assertion.distrox.DistroxStopStartScaleDurationAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class DistroXStopStartScaleTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
        createDefaultDatahub(testContext);
    }

    /**
     * The "Scaling" via Instance Stop/Start for DataHub ‘compute’ nodes (NodeManagers only) improves scaling performance
     * - at best, 4-6 minutes for a single node.
     *
     * @param testContext   Spring offers ApplicationContextAware interface to provide configuration of the Integration Test ApplicationContext.
     * @param iTestContext  TestNG offers the ITestContext interface to store and share test objects through test execution.
     */
    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "cluster has been scaled successfully by 4 compute nodes",
            then = "cluster compute nodes can be scaled down then up via stop then start instances at provider")
    public void testCreateAndScaleDistroX(TestContext testContext, ITestContext iTestContext) {
        AtomicReference<List<String>> instancesToStop = new AtomicReference<>();
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());

        if (params.getTimes() < 1) {
            throw new TestFailException("Test should execute at least 1 round of scaling");
        }

        testContext.given(DistroXTestDto.class)
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget()))
                .awaitForFlow()
                .then((tc, testDto, client) -> {
                    instancesToStop.set(distroxUtil.getInstanceIds(testDto, client, params.getHostGroup()).stream()
                            .limit(params.getScaleDownTarget()).collect(Collectors.toList()));
                    testDto.setRemovableInstanceIds(instancesToStop.get());
                    return testDto;
                })
                .when(distroXTestClient.scaleStopInstances())
                .await(STACK_AVAILABLE)
                .awaitForRemovableInstancesByState(InstanceStatus.STOPPED)
                .then(new DistroxStopStartScaleDurationAssertions(5, false))
                .when(distroXTestClient.scaleStartInstances(params.getHostGroup(), params.getScaleUpTarget()))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.get())
                .then(new DistroxStopStartScaleDurationAssertions(5, true));
        IntStream.range(1, params.getTimes())
                .forEach(i -> {
                            testContext
                                    .given(DistroXTestDto.class)
                                    .when(distroXTestClient.scaleStopInstances())
                                    .await(STACK_AVAILABLE)
                                    .awaitForRemovableInstancesByState(InstanceStatus.STOPPED)
                                    .then(new DistroxStopStartScaleDurationAssertions(5, false))
                                    .when(distroXTestClient.scaleStartInstances(params.getHostGroup(), params.getScaleUpTarget()))
                                    .await(STACK_AVAILABLE)
                                    .awaitForHealthyInstances()
                                    .when(distroXTestClient.get())
                                    .then(new DistroxStopStartScaleDurationAssertions(5, true));
                        }
                );
        testContext.given(DistroXTestDto.class).validate();
    }

}
