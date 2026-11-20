package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest.STACK_AVAILABLE;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest.STACK_DELETED;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

/**
 * AWS only scenario: a Data Hub created with the legacy AWS (CloudFormation) variant is migrated to AWS_NATIVE by a replace VMs runtime upgrade,
 * then scaled and deleted.
 * <p>
 * It runs on the environment and Data Lake of the calling test, so that the migration does not need a dedicated environment, FreeIPA and
 * Data Lake. The caller decides with {@link #isApplicable(TestContext)} whether to run it.
 */
@Component
public class AwsNativeMigrationOnSharedEnvironment {

    private static final String DATA_HUB_KEY = "awsNativeMigrationDataHub";

    private static final String AWS_VARIANT = "AWS";

    private static final String AWS_NATIVE_VARIANT = "AWS_NATIVE";

    private static final String COMPUTE_HOST_GROUP = "compute";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    public boolean isApplicable(TestContext testContext) {
        return CloudPlatform.AWS.equals(testContext.getCloudPlatform());
    }

    /**
     * Must be called before the caller changes the default {@code DistroXImageTestDto}, because a new Data Hub picks up the default image settings.
     */
    public void createLegacyAwsVariantDataHub(TestContext testContext, String runtimeVersion) {
        testContext
                .given(DATA_HUB_KEY, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataEngDistroXBlueprintName(runtimeVersion))
                .withVariant(AWS_VARIANT)
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .withLoadBalancer()
                .when(distroXTestClient.create(), key(DATA_HUB_KEY))
                .validate();
    }

    public void awaitLegacyAwsVariantDataHub(TestContext testContext) {
        testContext
                .given(DATA_HUB_KEY, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(DATA_HUB_KEY))
                .awaitForHealthyInstances()
                .when(distroXTestClient.checkVariant(AWS_VARIANT), key(DATA_HUB_KEY))
                .then((tc, testDto, client) -> assertCloudFormationStackExistence(tc, testDto, true), key(DATA_HUB_KEY))
                .validate();
    }

    /**
     * Triggers the upgrade without waiting for it, so it can run in parallel with the upgrades of the caller. The upgrade request is shared in the
     * test context, so replace VMs is switched back off for the upgrades the caller triggers afterwards.
     */
    public void triggerMigrationUpgrade(TestContext testContext, String targetRuntimeVersion) {
        testContext
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .withReplaceVms(DistroXUpgradeReplaceVms.ENABLED)
                .given(DATA_HUB_KEY, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(DATA_HUB_KEY))
                .given(DistroXUpgradeTestDto.class)
                .withReplaceVms(DistroXUpgradeReplaceVms.DISABLED)
                .validate();
    }

    public void assertMigrationThenScaleAndDelete(TestContext testContext) {
        testContext
                .given(DATA_HUB_KEY, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(DATA_HUB_KEY))
                .awaitForHealthyInstances()
                .when(distroXTestClient.checkVariant(AWS_NATIVE_VARIANT), key(DATA_HUB_KEY))
                .then((tc, testDto, client) -> assertCloudFormationStackExistence(tc, testDto, false), key(DATA_HUB_KEY))
                .when(distroXTestClient.scale(COMPUTE_HOST_GROUP, 3), key(DATA_HUB_KEY))
                .await(STACK_AVAILABLE, key(DATA_HUB_KEY))
                .awaitForHealthyInstances()
                .when(distroXTestClient.checkVariant(AWS_NATIVE_VARIANT), key(DATA_HUB_KEY))
                .when(distroXTestClient.scale(COMPUTE_HOST_GROUP, 1), key(DATA_HUB_KEY))
                .await(STACK_AVAILABLE, key(DATA_HUB_KEY))
                .awaitForHealthyInstances()
                .when(distroXTestClient.checkVariant(AWS_NATIVE_VARIANT), key(DATA_HUB_KEY))
                // the resources of the migrated AWS_NATIVE stack must be deletable without leftovers
                .when(distroXTestClient.delete(), key(DATA_HUB_KEY))
                .await(STACK_DELETED, key(DATA_HUB_KEY))
                .validate();
    }

    private DistroXTestDto assertCloudFormationStackExistence(TestContext testContext, DistroXTestDto testDto, boolean expected) {
        boolean exists = Boolean.TRUE.equals(testContext.getCloudProvider().getCloudFunctionality().isCloudFormationExistForStack(testDto.getName()));
        if (exists != expected) {
            throw new TestFailException(String.format("CloudFormation stack of DistroX %s %s", testDto.getName(),
                    expected ? "should exist but it does not" : "should not exist after the AWS_NATIVE migration but it does"));
        }
        return testDto;
    }
}
