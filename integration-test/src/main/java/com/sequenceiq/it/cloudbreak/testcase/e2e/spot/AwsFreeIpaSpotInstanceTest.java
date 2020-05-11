package com.sequenceiq.it.cloudbreak.testcase.e2e.spot;

import static org.testng.Assert.fail;

import java.util.Collection;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceLifeCycle;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class AwsFreeIpaSpotInstanceTest extends AbstractE2ETest {

    private static final SpotTestResultProvider RESULT_PROVIDER = new SpotTestResultProvider("FreeIpa");

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private FreeIPATestClient freeIPATestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        if (!CloudPlatform.AWS.name().equals(commonCloudProperties.getCloudProvider())) {
            fail(String.format("%s cloud provider is not supported for this test case!", commonCloudProperties.getCloudProvider()));
        }
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
                .given(FreeIPATestDto.class)
                    .withSpotPercentage(100)
                .when(freeIPATestClient.create())
                .await(Status.UPDATE_IN_PROGRESS)
                .when(freeIPATestClient.describe())
                .then(assertSpotInstances())
                .validate();
    }

    private Assertion<FreeIPATestDto, FreeIPAClient> assertSpotInstances() {
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
                && freeIpa.getStatusReason().contains(SpotTestResultProvider.AWS_INSUFFICIENT_SPOT_CAPACITY_MESSAGE);
    }

    private boolean allInstancesHaveSpotLifecycle(DescribeFreeIpaResponse freeIpa) {
        return freeIpa.getInstanceGroups().stream()
                .map(InstanceGroupResponse::getMetaData)
                .flatMap(Collection::stream)
                .allMatch(instance -> instance.getLifeCycle().equals(InstanceLifeCycle.SPOT));
    }
}
