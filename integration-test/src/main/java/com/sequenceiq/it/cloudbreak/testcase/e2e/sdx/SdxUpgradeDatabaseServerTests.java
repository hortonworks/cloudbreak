package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxUpgradeDatabaseServerTests extends PreconditionSdxE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeDatabaseServerTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUpgradeDatabaseTestUtil sdxUpgradeDatabaseTestUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "upgrade called on the SDX cluster",
            then = "SDX upgrade database server should be successful, the cluster should be up and running"
    )
    public void testSDXDatabaseUpgrade(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        sdxDatabaseRequest.setDatabaseEngineVersion(sdxUpgradeDatabaseTestUtil.getOriginalDatabaseMajorVersion());

        TargetMajorVersion targetDatabaseMajorVersion = sdxUpgradeDatabaseTestUtil.getTargetMajorVersion();

        testContext
                .given(sdx, SdxTestDto.class)
                    .withCloudStorage()
                    .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .given(SdxUpgradeDatabaseServerTestDto.class)
                    .withTargetMajorVersion(targetDatabaseMajorVersion)
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.upgradeDatabaseServer(), key(sdx))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_DATABASE_SERVER_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> sdxUpgradeDatabaseTestUtil.checkDbEngineVersionIsUpdated(targetDatabaseMajorVersion.getMajorVersion(), testDto))
                .then((tc, testDto, client) -> sdxUpgradeDatabaseTestUtil.checkCloudProviderDatabaseVersionFromMasterNode(
                        targetDatabaseMajorVersion.getMajorVersion(), tc, testDto))
                .validate();
    }
}
