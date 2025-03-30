package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_DB_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_SERVICE_SHARED_DB;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.GATEWAY_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.IDBROKER_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.LDAP_BIND_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.SALT_MASTER_KEY_PAIR;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.SALT_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.SALT_SIGN_KEY_PAIR;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.SSSD_IPA_PASSWORD;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.assertion.datalake.RecipeTestAssertion;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.SecretRotationCheckUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public class InternalSdxRepairWithRecipeTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalSdxRepairWithRecipeTest.class);

    private static final String FILEPATH = "/post-cm-start";

    private static final String FILENAME = "post-cm-start";

    private static final String MASTER_INSTANCEGROUP = "master";

    private static final String IDBROKER_INSTANCEGROUP = "idbroker";

    private static final String TELEMETRY = "telemetry";

    private String sdxInternal;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private SecretRotationCheckUtil secretRotationCheckUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is an environment with SDX in available state",
            when = "secrets are getting rotated (AWS - all secrets, non-AWS - limited set of secrets),  before " +
                    "recovery called on the IDBROKER and MASTER host group, where the instance had been stopped",
            then = "all the actions (secret rotation then recovery) should be successful, the cluster should be available"
    )
    public void testIDBRokerAndMasterRepairWithRecipeFile(TestContext testContext) {
        String cloudProvider = commonCloudProperties().getCloudProvider();

        createAutoTLSSdx(testContext);
        secretRotation(testContext, cloudProvider);
        multiRepairThenValidate(testContext);
    }

    private void createAutoTLSSdx(TestContext testContext) {
        sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String recipeName = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);

        testContext
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withBlueprintName(getDefaultSDXBlueprintName())
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .withContent(recipeUtil.generatePostCmStartRecipeContent(applicationContext))
                .withRecipeType(POST_CLOUDERA_MANAGER_START)
                .when(recipeTestClient.createV4())
                .given(MASTER_INSTANCEGROUP, InstanceGroupTestDto.class)
                .withHostGroup(MASTER)
                .withNodeCount(1)
                .withRecipes(recipeName)
                .given(IDBROKER_INSTANCEGROUP, InstanceGroupTestDto.class)
                .withHostGroup(IDBROKER)
                .withNodeCount(1)
                .withRecipes(recipeName)
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withInstanceGroups(MASTER_INSTANCEGROUP, IDBROKER_INSTANCEGROUP)
                .given(TELEMETRY, TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(sdxInternal, SdxInternalTestDto.class)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withDatabase(sdxDatabaseRequest)
                .withAutoTls()
                .withStackRequest(key(cluster), key(stack))
                .withTelemetry(TELEMETRY)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnHost(List.of(MASTER.getName(), IDBROKER.getName()), FILEPATH, FILENAME, 1, sshJUtil));
    }

    private void secretRotation(TestContext testContext, String cloudProvider) {
        String clusterName = testContext.given(sdxInternal, SdxInternalTestDto.class).getResponse().getName();
        Set<DatalakeSecretType> secretTypes = getAvailableSecretTypes(cloudProvider);
        SdxInternalTestDto sdxInternalTestDto = testContext
                .given(sdxInternal, SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternal(), key(sdxInternal))
                .when(sdxTestClient.rotateSecret(secretTypes))
                .awaitForFlow()
                .when(sdxTestClient.rotateSecret(Set.of(LDAP_BIND_PASSWORD)))
                .awaitForFlow()
                .then((tc, testDto, client) -> secretRotationCheckUtil.checkLdapLogin(testDto.getCrn(), testDto, client))
                .then((tc, testDto, client) -> secretRotationCheckUtil.preSaltPasswordRotation(testDto))
                .when(sdxTestClient.rotateSecret(Set.of(SALT_PASSWORD)))
                .awaitForFlow()
                .then((tc, testDto, client) -> secretRotationCheckUtil.validateSaltPasswordRotation(testDto));
    }

    private Set<DatalakeSecretType> getAvailableSecretTypes(String cloudProvider) {
        Set<DatalakeSecretType> secretTypes = Sets.newHashSet();
        secretTypes.addAll(Set.of(
                IDBROKER_CERT,
                EXTERNAL_DATABASE_ROOT_PASSWORD,
                CM_DB_PASSWORD,
                CM_INTERMEDIATE_CA_CERT,
                NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY));
        if (!CloudPlatform.GCP.equalsIgnoreCase(cloudProvider)) {
            // this is excluded due to CB-29204 (the rotation has a bug on GCP)
            secretTypes.add(SALT_BOOT_SECRETS);
            // these are excluded due to CB-29239 (to decrease execution time of test on GCP)
            secretTypes.addAll(Set.of(
                    GATEWAY_CERT,
                    CM_ADMIN_PASSWORD,
                    CM_SERVICES_DB_PASSWORD,
                    SALT_SIGN_KEY_PAIR,
                    SALT_MASTER_KEY_PAIR,
                    SSSD_IPA_PASSWORD,
                    CM_SERVICE_SHARED_DB));
        }
        return secretTypes;
    }

    private void multiRepairThenValidate(TestContext testContext) {
        testContext
                .given(sdxInternal, SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternal(), key(sdxInternal))
                .then((tc, testDto, client) -> {
                    List<String> instanceIdsToStop = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIdsToStop.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    getCloudFunctionality(tc).stopInstances(testDto.getName(), instanceIdsToStop);
                    return testDto;
                })
                .awaitForStoppedInstances()
                .when(sdxTestClient.repairInternal(MASTER.getName(), IDBROKER.getName()), key(sdxInternal))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnHost(List.of(MASTER.getName(), IDBROKER.getName()), FILEPATH, FILENAME, 1, sshJUtil))
                .validate();
    }
}
