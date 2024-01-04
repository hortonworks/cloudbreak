package com.sequenceiq.it.cloudbreak.testcase.e2e.spot;

import java.util.Collection;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class AwsDistroXSpotInstanceTest extends AbstractE2ETest {

    private static final SpotTestResultProvider RESULT_PROVIDER = new SpotTestResultProvider("DistroX");

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsDistroXSpotInstanceTest.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);
        createDatalakeWithoutDatabase(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak ",
            when = "creating a DistroX with 100% spot percentage ",
            then = "DistroX is started on spot instances, or fails with insufficient spot capacity"
    )
    public void testDistroXOnSpotInstances(TestContext testContext) {
        testContext
                .given(DistroXTestDto.class)
                    .withSpotPercentage(100)
                .when(distroXTestClient.create())
                .then((tc, testDto, client) -> {
                    testDto.awaitAndIgnoreFlows(STACK_CREATED);
                    Map<String, Exception> exceptionMap = testContext.getExceptionMap();
                    if (!exceptionMap.isEmpty()) {
                        String key = testDto.getAwaitExceptionKey(STACK_CREATED);
                        if (exceptionMap.containsKey(key)) {
                            LOGGER.info("Awaiting STACK_CREATED failed, clearing exception to check status reason", exceptionMap.get(key));
                            exceptionMap.remove(key);
                        }
                    }
                    return testDto;
                })
                .when(distroXTestClient.get())
                .then(assertSpotInstances())
                .validate();
    }

    private Assertion<DistroXTestDto, CloudbreakClient> assertSpotInstances() {
        return (tc, testDto, client) -> {
            StackV4Response stack = testDto.getResponse();
            if (createFailedWithInsufficientInstanceCapacity(stack)) {
                RESULT_PROVIDER.insufficientCapacity();
            } else if (allInstancesHaveSpotLifecycle(stack)) {
                RESULT_PROVIDER.runsOnSpotInstances();
            } else {
                RESULT_PROVIDER.fail(stack);
            }
            return testDto;
        };
    }

    private boolean createFailedWithInsufficientInstanceCapacity(StackV4Response stack) {
        return stack.getStatus().equals(Status.CREATE_FAILED)
                && RESULT_PROVIDER.isSpotFailureStatusReason(stack.getStatusReason());
    }

    private boolean allInstancesHaveSpotLifecycle(StackV4Response stack) {
        return stack.getInstanceGroups().stream()
                .map(InstanceGroupV4Response::getMetadata)
                .flatMap(Collection::stream)
                .allMatch(instance -> InstanceLifeCycle.SPOT.equals(instance.getLifeCycle()));
    }
}
