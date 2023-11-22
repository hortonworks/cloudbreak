package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_MGMT_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.GATEWAY_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.USER_KEYPAIR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class DistroXRepairTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRepairTests.class);

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
        createDefaultDatahubWithAutoTlsAndExternalDb(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an environment and DistroX with Auto TLS in available state",
            when = "in case of AWS provider secrets are getting rotated before " +
                    "recovery called on the MASTER host group, where the instance had been " +
                    "terminated",
            then = "all the actions (secret rotatioin then recovery for AWS OR just recovery) should be successful, " +
                    "the cluster should be available"
    )
    public void testMasterRepairWithTerminatedInstances(TestContext testContext) {
        String cloudProvider = commonCloudProperties().getCloudProvider();

        if (CloudPlatform.AWS.equalsIgnoreCase(cloudProvider)) {
            secretRotation(testContext);
            masterRepairValidate(testContext);
        } else {
            masterRepairValidate(testContext);
        }
    }

    private void secretRotation(TestContext testContext) {
        testContext
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(
                        USER_KEYPAIR,
                        GATEWAY_CERT,
                        SALT_BOOT_SECRETS,
                        CLUSTER_MGMT_CM_ADMIN_PASSWORD,
                        CLUSTER_CB_CM_ADMIN_PASSWORD,
                        CLUSTER_CM_DB_PASSWORD,
                        CLUSTER_CM_SERVICES_DB_PASSWORD,
                        DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD,
                        DATAHUB_CM_INTERMEDIATE_CA_CERT)))
                .awaitForFlow();
    }

    private void masterRepairValidate(TestContext testContext) {
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(DistroXTestDto.class)
                .then(new AwsAvailabilityZoneAssertion())
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instancesToDelete = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    expectedVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instancesToDelete));
                    cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroup(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .when(distroXTestClient.repair(MASTER))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion())
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instanceIds = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    actualVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }
}
