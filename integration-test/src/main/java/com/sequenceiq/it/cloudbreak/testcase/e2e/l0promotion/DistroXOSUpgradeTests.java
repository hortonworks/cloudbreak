package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

public class DistroXOSUpgradeTests extends AbstractE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Ignore("This test case should be re-enabled in case of OPSAPS-62124 has been resolved at MOW-Dev")
    @Test(dataProvider = TEST_CONTEXT, description = "We need to wait the OPSAPS-62124 to be resolved")
    @Description(
            given = "there is a running environment with freeIPA in available state",
            when = "SDX (light duty) and base DistroX (5 nodes with master, compute and workers) should be created successfully",
            and = "SDX then DistroX runtime upgrade done successfully after that OS upgrade called on DistroX",
            then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testBaseDistroXOSUpgrade(TestContext testContext) {
        String sdxName = resourcePropertyProvider().getName();
        String distroXName = resourcePropertyProvider().getName();
        String currentRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion();
        String targetRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeTargetVersion();

        testContext
                .given(sdxName, SdxTestDto.class)
                    .withCloudStorage()
                    .withRuntimeVersion(currentRuntimeVersion)
                .when(sdxTestClient.create(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();

        testContext
                .given(distroXName, DistroXTestDto.class)
                    .withTemplate(String.format(commonClusterManagerProperties.getInternalDistroXBlueprintType(), currentRuntimeVersion))
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();

        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.stop(), key(distroXName))
                .await(STACK_STOPPED, key(distroXName))
                .validate();
        testContext
                .given(SdxUpgradeTestDto.class)
                    .withReplaceVms(SdxUpgradeReplaceVms.ENABLED)
                    .withRuntime(targetRuntimeVersion)
                .given(sdxName, SdxTestDto.class)
                .when(sdxTestClient.upgrade(), key(sdxName))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();

        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.start(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .validate();
        testContext
                .given(DistroXUpgradeTestDto.class)
                    .withRuntime(targetRuntimeVersion)
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(DistroXUpgradeTestDto.class)
                    .withLockComponents(Boolean.TRUE)
                    .withRuntime(null)
                    .withReplaceVms(DistroXUpgradeReplaceVms.ENABLED)
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();

        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(sdxName, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.get(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();
    }
}
