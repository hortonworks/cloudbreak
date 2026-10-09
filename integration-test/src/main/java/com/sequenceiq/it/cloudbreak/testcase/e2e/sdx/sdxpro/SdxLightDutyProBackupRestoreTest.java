package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.sdxpro;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.backup.SdxBackupRestoreValidator;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxLightDutyProBackupRestoreTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private SdxBackupRestoreValidator sdxBackupRestoreValidator;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        String runtimeVersion = commonClusterManagerProperties().getProShapeRuntimeVersion();
        String qualifiedImageId = getQualifiedPrewarmedImageId(testContext, runtimeVersion,
                commonClusterManagerProperties().getProShapeMinimumQualifiedRuntimeVersion());
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);
        testContext
                .given(UmsTestDto.class)
                .when(umsTestClient.grantEntitlement(testContext.getActingUserCrn().getAccountId(),
                        Entitlement.CDP_DATALAKE_SHAPES_WITHOUT_HBASE_AND_HDFS.name()));
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(SdxInternalTestDto.class)
                .withTelemetry("telemetry")
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withRuntimeVersion(runtimeVersion)
                .withImageCatalogNameAndImageId(commonCloudProperties().getImageCatalogName(), qualifiedImageId)
                .withClusterShape(SdxClusterShape.LIGHT_DUTY_PRO)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Light Duty Pro SDX cluster in available state",
            when = "a basic SDX backup then restore request has been sent",
            then = "SDX restore should be done successfully"
    )
    public void testSdxLightDutyProBackupRestoreCanBeSuccessful(TestContext testContext) {
        SdxInternalTestDto sdxInternalTestDto = testContext.given(SdxInternalTestDto.class);
        String cloudStorageBaseLocation = sdxInternalTestDto.getResponse().getCloudStorageBaseLocation();
        String backupObject = "backups";
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.syncInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.backupInternal(StringUtils.join(List.of(cloudStorageBaseLocation, backupObject), "/"), null))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(sdxBackupRestoreValidator::validateDatalakeBackupStatus)
                .then(sdxBackupRestoreValidator::validateDatalakeStatus)
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainer(cloudStorageBaseLocation, backupObject, true);
                    String databaseBackupLocation = cloudStorageBaseLocation + '/' + backupObject + '/' + sdxBackupRestoreValidator.getBackupId()
                            + "_database_backup";
                    getCloudFunctionality(tc).cloudStorageListContainer(databaseBackupLocation, "hive_backup", true);
                    getCloudFunctionality(tc).cloudStorageListContainer(databaseBackupLocation, "ranger_backup", true);
                    return testDto;
                })
                .validate();

        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.restoreInternal(sdxBackupRestoreValidator.getBackupId(), null))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(sdxBackupRestoreValidator::validateDatalakeRestoreStatus)
                .then(sdxBackupRestoreValidator::validateDatalakeStatus)
                .validate();
    }
}
