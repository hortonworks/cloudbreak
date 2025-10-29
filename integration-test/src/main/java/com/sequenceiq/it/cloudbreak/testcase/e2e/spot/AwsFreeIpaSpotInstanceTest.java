package com.sequenceiq.it.cloudbreak.testcase.e2e.spot;

import java.util.Collection;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceLifeCycle;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class AwsFreeIpaSpotInstanceTest extends AbstractE2ETest {

    private static final SpotTestResultProvider RESULT_PROVIDER = new SpotTestResultProvider("FreeIpa");

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsFreeIpaSpotInstanceTest.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);
        super.setupTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak ",
            when = "creating a FreeIpa with 100% spot percentage ",
            then = "FreeIpa is started on spot instances, or fails with insufficient spot capacity"
    )
    public void testFreeIpaOnSpotInstances(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                    .withSpotPercentage(100)
                .when(freeIpaTestClient.create())
                .then((tc, testDto, client) -> {
                    testDto.await(Status.UPDATE_IN_PROGRESS);
                    Map<String, Exception> exceptionMap = testContext.getExceptionMap();
                    if (!exceptionMap.isEmpty()) {
                        String key = testDto.getAwaitExceptionKey(Status.UPDATE_IN_PROGRESS);
                        if (exceptionMap.containsKey(key)) {
                            LOGGER.info("Awaiting UPDATE_IN_PROGRESS failed, clearing exception to check status reason", exceptionMap.get(key));
                            exceptionMap.remove(key);
                        }
                    }
                    return testDto;
                })
                .when(freeIpaTestClient.describe())
                .then(assertSpotInstances())
                .validate();
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> assertSpotInstances() {
        return (tc, testDto, client) -> {
            DescribeFreeIpaResponse freeIpa = testDto.getResponse();
            if (createFailedWithInsufficientInstanceCapacity(freeIpa)) {
                RESULT_PROVIDER.insufficientCapacity();
            } else if (allInstancesHaveSpotLifecycle(freeIpa)) {
                RESULT_PROVIDER.runsOnSpotInstances();
            } else {
                RESULT_PROVIDER.fail(freeIpa);
            }
            return testDto;
        };
    }

    private boolean createFailedWithInsufficientInstanceCapacity(DescribeFreeIpaResponse freeIpa) {
        return freeIpa.getStatus().equals(Status.CREATE_FAILED)
                && RESULT_PROVIDER.isSpotFailureStatusReason(freeIpa.getStatusReason());
    }

    private boolean allInstancesHaveSpotLifecycle(DescribeFreeIpaResponse freeIpa) {
        return freeIpa.getInstanceGroups().stream()
                .map(InstanceGroupResponse::getMetaData)
                .flatMap(Collection::stream)
                .allMatch(instance -> InstanceLifeCycle.SPOT.equals(instance.getLifeCycle()));
    }
}
