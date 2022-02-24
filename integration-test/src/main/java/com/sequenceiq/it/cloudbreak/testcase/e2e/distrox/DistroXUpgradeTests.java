package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class DistroXUpgradeTests extends AbstractE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "upgrade called on the DistroX cluster",
            then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testDistroXUpgrade(TestContext testContext) {

        String sdxName = resourcePropertyProvider().getName();
        String distroXName = resourcePropertyProvider().getName();
        String currentRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion();
        String targetRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeTargetVersion();
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        testContext
                .given(sdxName, SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(currentRuntimeVersion)
                .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .withTemplate(String.format(commonClusterManagerProperties.getInternalDistroXBlueprintType(), currentRuntimeVersion))
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion())
                .validate();
        testContext
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion())
                .validate();
    }
}
