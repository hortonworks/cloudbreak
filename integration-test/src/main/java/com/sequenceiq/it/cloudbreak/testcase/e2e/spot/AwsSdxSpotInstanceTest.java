package com.sequenceiq.it.cloudbreak.testcase.e2e.spot;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static org.testng.Assert.fail;

import java.util.Collection;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class AwsSdxSpotInstanceTest extends AbstractE2ETest {

    private static final SpotTestResultProvider RESULT_PROVIDER = new SpotTestResultProvider("SDX");

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        if (!CloudPlatform.AWS.name().equals(commonCloudProperties.getCloudProvider())) {
            fail(String.format("%s cloud provider is not supported for this test case!", commonCloudProperties.getCloudProvider()));
        }
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak ",
            when = "creating an SDX with 100% spot percentage ",
            then = "SDX is started on spot instances, or fails with insufficient spot capacity"
    )
    public void testSdxOnSpotInstances(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        SdxDatabaseRequest database = new SdxDatabaseRequest();
        database.setCreate(false);

        testContext
                .given(SdxTestDto.class)
                    .withExternalDatabase(database)
                    .withSpotPercentage(100)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdx))
                .wait(STACK_CREATED, key(sdx))
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
                && stack.getStatusReason().contains(SpotTestResultProvider.AWS_INSUFFICIENT_SPOT_CAPACITY_MESSAGE);
    }

    private boolean allInstancesHaveSpotLifecycle(StackV4Response stack) {
        return stack.getInstanceGroups().stream()
                .map(InstanceGroupV4Response::getMetadata)
                .flatMap(Collection::stream)
                .allMatch(instance -> instance.getLifeCycle().equals(InstanceLifeCycle.SPOT));
    }
}
