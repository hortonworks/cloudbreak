package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_MGMT_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.GATEWAY_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.IDBROKER_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.USER_KEYPAIR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_DB_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_SERVICE_DB_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_GATEWAY_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_IDBROKER_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_MGMT_CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_SALT_BOOT_SECRETS;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_USER_KEYPAIR;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class DistroXRepairTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRepairTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an environment with DistroX in available state",
            when = "secrets are getting rotated",
                and = "recovery called on the MASTER host group, where the instance had been terminated",
            then = "rotation then recovery should be successful, the cluster should be available"
    )
    public void testSecretRotationThenMasterRepair(TestContext testContext) {
        String distrox = resourcePropertyProvider().getName();
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(distrox, DistroXTestDto.class)
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .when(distroXTestClient.create(), key(distrox))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion())
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(
                        DATALAKE_USER_KEYPAIR,
                        DATALAKE_IDBROKER_CERT,
                        DATALAKE_GATEWAY_CERT,
                        DATALAKE_SALT_BOOT_SECRETS,
                        DATALAKE_MGMT_CM_ADMIN_PASSWORD,
                        DATALAKE_CB_CM_ADMIN_PASSWORD,
                        DATALAKE_DATABASE_ROOT_PASSWORD,
                        DATALAKE_CM_DB_PASSWORD,
                        DATALAKE_CM_SERVICE_DB_PASSWORD)))
                .awaitForFlow()
                .given(distrox, DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(
                        USER_KEYPAIR,
                        IDBROKER_CERT,
                        GATEWAY_CERT,
                        SALT_BOOT_SECRETS,
                        CLUSTER_MGMT_CM_ADMIN_PASSWORD,
                        CLUSTER_CB_CM_ADMIN_PASSWORD,
                        CLUSTER_CM_DB_PASSWORD,
                        CLUSTER_CM_SERVICES_DB_PASSWORD,
                        DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD)))
                .awaitForFlow()
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instancesToDelete = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    expectedVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instancesToDelete));
                    cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroup(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .when(distroXTestClient.repair(MASTER), key(distrox))
                .await(STACK_AVAILABLE, key(distrox))
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion())
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instanceIds = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    actualVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds,
                        expectedVolumeIds))
                .validate();
    }
}
