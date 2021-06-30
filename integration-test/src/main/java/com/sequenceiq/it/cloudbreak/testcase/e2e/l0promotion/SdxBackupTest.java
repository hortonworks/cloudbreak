package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static java.lang.String.format;

import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxBackupTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Manowar SDX cluster in available state",
            when = "a basic SDX create request with FreeIPA and DataLake Cloud Storage has been sent",
            then = "SDX should be available along with the created Cloud storage objects"
    )
    public void testSDXBackupCanBeCreatedSuccessfully(TestContext testContext) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        testContext
                .given(SdxInternalTestDto.class)
                    .withDatabase(sdxDatabaseRequest)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();

        SdxInternalTestDto sdxInternalTestDto = testContext.given(SdxInternalTestDto.class);
        String cloudStorageBaseLocation = sdxInternalTestDto.getResponse().getCloudStorageBaseLocation();
        String backupLocation = cloudStorageBaseLocation + "/backups";
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.backup(backupLocation, null))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(this::validateDatalakeBackupStatus)
                .then(this::validateDatalakeStatus)
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainer(cloudStorageBaseLocation, backupLocation, false);
                    return testDto;
                })
                .validate();
    }

    private SdxInternalTestDto validateDatalakeStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String statuReason = client.getDefaultClient()
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStatusReason();
        if (statuReason.contains("Datalake backup failed")) {
            LOGGER.error(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statuReason));
            throw new TestFailException(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statuReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statuReason));
            Log.then(LOGGER, format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statuReason));
        }
        return testDto;
    }

    private SdxInternalTestDto validateDatalakeBackupStatus(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        String sdxName = testDto.getName();
        SdxBackupStatusResponse sdxBackupStatusResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getBackupDatalakeStatus(sdxName, null, null);
        String status = sdxBackupStatusResponse.getStatus();
        String statuReason = sdxBackupStatusResponse.getReason();
        LOGGER.info(format(" SDX '%s' backup status '%s', because of %s ", sdxName, status, statuReason));
        if (status.contains("FAILED")) {
            LOGGER.error(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statuReason));
            throw new TestFailException(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statuReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statuReason));
            Log.then(LOGGER, format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statuReason));
        }
        return testDto;
    }
}
