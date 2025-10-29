package com.sequenceiq.it.cloudbreak.testcase.e2e.spot;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

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
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class AwsSdxSpotInstanceTest extends AbstractE2ETest {

    private static final SpotTestResultProvider RESULT_PROVIDER = new SpotTestResultProvider("SDX");

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSdxSpotInstanceTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak ",
            when = "creating an SDX with 100% spot percentage ",
            then = "SDX is started on spot instances, or fails with insufficient spot capacity"
    )
    public void testSdxOnSpotInstances(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(SdxTestDto.class)
                    .withCloudStorage()
                    .withSpotPercentage(100)
                .when(sdxTestClient.create(), key(sdx))
                .then((tc, testDto, client) -> {
                    testDto.await(STACK_CREATED, key(sdx));
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
                .when(sdxTestClient.describe())
                .then(assertSpotInstances())
                .validate();
    }

    private Assertion<SdxTestDto, SdxClient> assertSpotInstances() {
        return (tc, testDto, client) -> {
            StackV4Response stack = testDto.getResponse().getStackV4Response();
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
