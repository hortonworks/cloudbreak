package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.LDAP_BIND_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_SIGN_KEY_PAIR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.SecretRotationCheckUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public class DistroXRepairTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRepairTests.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private SecretRotationCheckUtil secretRotationCheckUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an environment and DistroX with Auto TLS in available state",
            when = "secrets are getting rotated (AWS - all secrets, non-AWS - limited set of secrets) before " +
                    "recovery called on the MASTER host group, where the instance had been terminated",
            then = "all the actions (secret rotation then recovery) should be successful, the cluster should be available"
    )
    public void testSecretRotationAndMasterRepairWithTerminatedInstances(TestContext testContext) {
        createDataMartDatahubWithAutoTlsAndExternalDb(testContext);
        String cloudProvider = commonCloudProperties().getCloudProvider();

        secretRotation(testContext, cloudProvider);
        masterRepairValidate(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an environment and DistroX with AWS_NATIVE variant and Auto TLS in available state",
            when = "secrets are getting rotated (AWS - all secrets, non-AWS - limited set of secrets) before " +
                    "recovery called on the MASTER host group, where the instance had been terminated",
            then = "all the actions (secret rotation then recovery) should be successful, the cluster should be available"
    )
    public void testSecretRotationAndMasterRepairWithTerminatedInstancesAwsNative(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);

        testContext
                .given(SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withVariant("AWS_NATIVE")
                .withRuntimeVersion(commonClusterManagerProperties.getUpgrade().getTargetRuntimeVersion())
                .withAutoTls()
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withEnableMultiAz()
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .validate();
        waitForEnvironmentCreation(testContext);
        setFreeIpaResponse(testContext);
        waitForUserSync(testContext);
        setFreeIpaResponse(testContext);
        waitForDatalakeCreation(testContext);
        DistroXDatabaseRequest distroxDatabaseRequest = new DistroXDatabaseRequest();
        distroxDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
                .given(DistroXTestDto.class)
                .withAutoTls()
                .withVariant("AWS_NATIVE")
                .withExternalDatabase(distroxDatabaseRequest)
                .withCluster(testContext.given(DistroXClusterTestDto.class)
                        .withBlueprintName(commonClusterManagerProperties
                                .getDataMartDistroXBlueprintName(commonClusterManagerProperties.getUpgrade().getTargetRuntimeVersion())))
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataMartHostGroups(testContext, testContext.getCloudPlatform()))
                .when(distroXTestClient.create())
                .validate();
        waitForDatahubCreation(testContext);
        String cloudProvider = commonCloudProperties().getCloudProvider();

        secretRotation(testContext, cloudProvider);
        masterRepairValidate(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an environment and DistroX with AWS variant and Auto TLS in available state",
            when = "secrets are getting rotated (AWS - all secrets, non-AWS - limited set of secrets) before " +
                    "recovery called on the MASTER host group, where the instance had been terminated",
            then = "all the actions (secret rotation then recovery) should be successful, the cluster should be available"
    )
    public void testSecretRotationAndMasterRepairWithTerminatedInstancesAws(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);

        testContext
                .given(SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withVariant("AWS")
                .withRuntimeVersion(commonClusterManagerProperties.getUpgrade()
                        .getDistroXUpgradeCurrentVersion(testContext.getCloudProvider().getGovCloud()))
                .withAutoTls()
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withEnableMultiAz()
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .validate();
        waitForEnvironmentCreation(testContext);
        setFreeIpaResponse(testContext);
        waitForUserSync(testContext);
        setFreeIpaResponse(testContext);
        waitForDatalakeCreation(testContext);
        DistroXDatabaseRequest distroxDatabaseRequest = new DistroXDatabaseRequest();
        distroxDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
                .given(DistroXTestDto.class)
                .withAutoTls()
                .withVariant("AWS")
                .withExternalDatabase(distroxDatabaseRequest)
                .withCluster(testContext.given(DistroXClusterTestDto.class)
                        .withBlueprintName(testContext.getCloudProvider().getDataMartDistroXBlueprintName()))
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataMartHostGroups(testContext, testContext.getCloudPlatform()))
                .when(distroXTestClient.create())
                .validate();
        waitForDatahubCreation(testContext);
        String cloudProvider = commonCloudProperties().getCloudProvider();

        secretRotation(testContext, cloudProvider);
        masterRepairValidate(testContext);
    }

    private void secretRotation(TestContext testContext, String cloudProvider) {
        testContext.given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DatalakeSecretType.CM_SERVICE_SHARED_DB)))
                .awaitForFlow()
                .validate();
        String clusterName = testContext.given(DistroXTestDto.class).getResponse().getName();
        Set<CloudbreakSecretType> secretTypes = getAvailableSecretTypes(cloudProvider);
        DistroXTestDto distroXTestDto = testContext
                .given(DistroXTestDto.class)
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.rotateSecret(secretTypes))
                .awaitForFlow()
                .when(distroXTestClient.rotateSecret(Set.of(LDAP_BIND_PASSWORD)))
                .awaitForFlow()
                .then((tc, testDto, client) -> secretRotationCheckUtil.checkLdapLogin(tc, testDto, client))
                .then((tc, testDto, client) -> secretRotationCheckUtil.preSaltPasswordRotation(testDto))
                .when(distroXTestClient.rotateSecret(Set.of(SALT_PASSWORD)))
                .awaitForFlow()
                .then((tc, testDto, client) -> secretRotationCheckUtil.validateSaltPasswordRotation(testDto));
    }

    private Set<CloudbreakSecretType> getAvailableSecretTypes(String cloudProvider) {
        Set<CloudbreakSecretType> secretTypes = Sets.newHashSet();
        secretTypes.addAll(Set.of(
                SALT_MASTER_KEY_PAIR,
                SALT_SIGN_KEY_PAIR,
                CM_ADMIN_PASSWORD,
                CM_DB_PASSWORD,
                // CDPAM-5911
                //GATEWAY_CERT,
                EXTERNAL_DATABASE_ROOT_PASSWORD,
                CM_INTERMEDIATE_CA_CERT,
                NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY,
                CM_SERVICES_DB_PASSWORD));
        if (!CloudPlatform.GCP.equalsIgnoreCase(cloudProvider)) {
            secretTypes.add(SALT_BOOT_SECRETS);
        }
        return secretTypes;
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