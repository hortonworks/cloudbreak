package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxKraftMigrationTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        testContext.given(SdxTestDto.class)
                .withClusterShape(SdxClusterShape.LIGHT_DUTY)
                .withCloudStorage()
                .withRuntimeVersion("7.3.2")
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "Kraft migration is performed on the SDX cluster",
            then = "the SDX cluster should be available and running in KRaft mode")
    public void testSdxKraftMigrationWithRollback(TestContext testContext) {
        testContext
                .given(SdxTestDto.class)
                .when(sdxTestClient.validateKraftMigrationStatus(ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE.name()))
                .when(sdxTestClient.startKraftMigration())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.validateKraftMigrationStatus(ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE.name()))
                .when(sdxTestClient.rollbackKraftMigration())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.validateKraftMigrationStatus(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE.name()))
                .when(sdxTestClient.startKraftMigration())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.validateKraftMigrationStatus(ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE.name()))
                .when(sdxTestClient.finalizeKraftMigration())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.validateKraftMigrationStatus(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE.name()))
                .validate();
    }
}
