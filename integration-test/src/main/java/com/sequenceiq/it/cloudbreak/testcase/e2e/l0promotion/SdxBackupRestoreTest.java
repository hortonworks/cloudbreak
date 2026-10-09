package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.backup.SdxBackupRestoreValidator;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxBackupRestoreTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxBackupRestoreValidator sdxBackupRestoreValidator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Manowar Data Lake cluster in available state",
            when = "a basic Data Lake backup then restore request has been sent",
            then = "Data Lake restore should be done successfully"
    )
    public void testSDXBackupRestoreCanBeSuccessful(TestContext testContext) {
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
