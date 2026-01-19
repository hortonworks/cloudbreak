package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreStatusResponse;

public class SdxBackupRestoreTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupRestoreTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    private String backupId;

    private String restoreId;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Manowar SDX cluster in available state",
            when = "a basic SDX backup then restore request has been sent",
            then = "SDX restore should be done successfully"
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
                .then(this::validateDatalakeBackupStatus)
                .then(this::validateDatalakeStatus)
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainer(cloudStorageBaseLocation, backupObject, true);
                    String databaseBackupLocation = cloudStorageBaseLocation + '/' + backupObject + '/' + backupId + "_database_backup";
                    getCloudFunctionality(tc).cloudStorageListContainer(databaseBackupLocation, "hive_backup", true);
                    getCloudFunctionality(tc).cloudStorageListContainer(databaseBackupLocation, "ranger_backup", true);
                    return testDto;
                })
                .validate();

        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.restoreInternal(backupId, null))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(this::validateDatalakeRestoreStatus)
                .then(this::validateDatalakeStatus)
                .validate();
    }

    private SdxInternalTestDto validateDatalakeStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String statusReason = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStatusReason();
        if (statusReason.contains("Datalake backup failed")) {
            LOGGER.error(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
            throw new TestFailException(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
        } else if (statusReason.contains("Datalake restore failed")) {
            LOGGER.error(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
            throw new TestFailException(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' backup/restore has been done with '%s'. ", testDto.getName(), statusReason));
            Log.then(LOGGER, format(" Sdx '%s' backup/restore has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }

    private SdxInternalTestDto validateDatalakeBackupStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String sdxName = testDto.getName();
        backupId = client.getDefaultClient(testContext)
                .sdxBackupEndpoint()
                .getDatalakeBackupId(sdxName, null);
        SdxBackupStatusResponse sdxBackupStatusResponse = client.getDefaultClient(testContext)
                .sdxBackupEndpoint()
                .getBackupDatalakeStatus(sdxName, backupId, null);
        String status = sdxBackupStatusResponse.getStatus();
        String statusReason = sdxBackupStatusResponse.getReason();
        LOGGER.info(format(" SDX '%s' backup '%s' status '%s', because of %s ", sdxName, backupId, status, statusReason));
        if (status.contains("FAILED")) {
            LOGGER.error(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
            throw new TestFailException(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statusReason));
            Log.then(LOGGER, format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }

    private SdxInternalTestDto validateDatalakeRestoreStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String sdxName = testDto.getName();
        String status;
        String statusReason;

        try {
            restoreId = client.getDefaultClient(testContext)
                    .sdxRestoreEndpoint()
                    .getDatalakeRestoreId(sdxName, null);
            SdxRestoreStatusResponse sdxRestoreStatusResponse = client.getDefaultClient(testContext)
                    .sdxRestoreEndpoint()
                    .getRestoreDatalakeStatus(sdxName, restoreId, null);
            status = sdxRestoreStatusResponse.getStatus();
            statusReason = sdxRestoreStatusResponse.getReason();
            LOGGER.info(format(" SDX '%s' restore '%s' status '%s', because of %s ", sdxName, restoreId, status, statusReason));
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                throw new TestFailException(String.format(" NOT FOUND :: Cannot get status information for restore '%s' on datalake '%s'." +
                        " Please check the selected backup was successful and the related backup ID is correct. ",
                        restoreId, testDto.getName()), e.getCause());
            }
            throw e;
        }

        if (StringUtils.isBlank(status)) {
            LOGGER.error(String.format(" Sdx '%s' restore status is not available ", testDto.getName()));
            throw new TestFailException(String.format(" Sdx '%s' restore status is not available  ", testDto.getName()));
        } else if (status.contains("FAILED")) {
            LOGGER.error(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
            throw new TestFailException(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' restore has been done with '%s'. ", testDto.getName(), statusReason));
            Log.then(LOGGER, format(" Sdx '%s' restore has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }
}
