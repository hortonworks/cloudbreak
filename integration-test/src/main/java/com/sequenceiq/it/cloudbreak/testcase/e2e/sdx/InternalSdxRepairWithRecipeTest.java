package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.datalake.RecipeTestAssertion;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class InternalSdxRepairWithRecipeTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalSdxRepairWithRecipeTest.class);

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

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is an environment with SDX in available state",
            when = "secrets are getting rotated",
                and = "recovery called on the IDBROKER and MASTER host group, where the instance had been stopped",
            then = "rotation then recovery should be successful, the cluster should be available"
    )
    public void testSecretRotationThenMultiRepairWithRecipeFile(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String recipeName = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String filePath = "/post-cm-start";
        String fileName = "post-cm-start";
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";
        String telemetry = "telemetry";

        String selectedImageID = getLatestPrewarmedImageId(testContext);

        testContext
                .given(imageSettings, ImageSettingsTestDto.class)
                    .withImageCatalog(commonCloudProperties().getImageCatalogName())
                    .withImageId(selectedImageID)
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
                .given(masterInstanceGroup, InstanceGroupTestDto.class)
                    .withHostGroup(MASTER)
                    .withNodeCount(1)
                    .withRecipes(recipeName)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class)
                    .withHostGroup(IDBROKER)
                    .withNodeCount(1)
                    .withRecipes(recipeName)
                .given(stack, StackTestDto.class)
                    .withCluster(cluster)
                    .withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup)
                    .withImageSettings(imageSettings)
                .given(telemetry, TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(sdxInternal, SdxInternalTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withStackRequest(key(cluster), key(stack))
                    .withTelemetry(telemetry)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnHost(List.of(MASTER.getName(), IDBROKER.getName()),
                        filePath, fileName, 1, sshJUtil))
                .then((tc, dto, client) -> {
                    if (!StringUtils.equalsIgnoreCase(dto.getResponse().getStackV4Response().getImage().getId(),
                            selectedImageID)) {
                        throw new TestFailException(
                                String.format("The datalake image Id (%s) do NOT match with the selected" +
                                                " pre-warmed image Id: '%s'!",
                                dto.getResponse().getStackV4Response().getImage().getId(), selectedImageID));
                    }
                    return dto;
                })
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
                .then(RecipeTestAssertion.validateFilesOnHost(List.of(MASTER.getName(), IDBROKER.getName()), filePath,
                        fileName, 1, sshJUtil))
                .validate();
    }
}
