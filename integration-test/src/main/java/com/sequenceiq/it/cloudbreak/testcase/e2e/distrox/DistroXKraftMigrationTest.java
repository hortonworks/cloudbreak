package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.KRAFT;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2EWithReusableResourcesTest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXKraftMigrationTest extends AbstractE2EWithReusableResourcesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXKraftMigrationTest.class);

    private static final String STREAMS_MESSAGING_LIGHT_DUTY_TEMPLATE = "7.3.2 - Streams Messaging Light Duty: Apache Kafka, Schema Registry," +
            " Streams Messaging Manager, Streams Replication Manager, Cruise Control";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupClass(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        testContext
                .given(SdxTestDto.class)
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
            when = "Streams Messaging Datahub is provisioned, and Kraft migration is performed",
            then = "the Streams Messaging Datahub should be available and running in KRaft mode")
    public void testKraftMigrationWithNodesInTemplate(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(DistroXTestDto.class)
                .withTemplate(STREAMS_MESSAGING_LIGHT_DUTY_TEMPLATE)
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.streamsHostGroups(testContext, testContext.getCloudPlatform()))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name()))
                .when(distroXTestClient.startKraftMigration())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.BROKERS_IN_KRAFT.name()))
                .when(distroXTestClient.finalizeKraftMigration())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.KRAFT_INSTALLED.name()))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "Streams Messaging Datahub is provisioned, Kraft migration, rollback then migration is performed",
            then = "the Streams Messaging Datahub should be available and running in KRaft mode")
    public void testKraftMigrationRollback(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(DistroXTestDto.class)
                .withTemplate(STREAMS_MESSAGING_LIGHT_DUTY_TEMPLATE)
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.streamsHostGroups(testContext, testContext.getCloudPlatform()))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name()))
                .when(distroXTestClient.startKraftMigration())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.BROKERS_IN_KRAFT.name()))
                .when(distroXTestClient.rollbackKraftMigration())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.PRE_MIGRATION.name()))
                .when(distroXTestClient.startKraftMigration())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.BROKERS_IN_KRAFT.name()))
                .when(distroXTestClient.finalizeKraftMigration())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.KRAFT_INSTALLED.name()))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "Streams Messaging Datahub is provisioned, Kraft migration fails at upscale, then repair is performed and migration is retried",
            then = "the Streams Messaging Datahub should be available and running in KRaft mode")
    public void testKraftMigrationFailAtUpscale(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(DistroXTestDto.class)
                .withTemplate(STREAMS_MESSAGING_LIGHT_DUTY_TEMPLATE)
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.streamsHostGroups(testContext, testContext.getCloudPlatform()))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.startKraftMigration())
                .await(UPDATE_IN_PROGRESS, RunningParameter.emptyRunningParameter().withoutWaitForFlow())
                .awaitForCurrentlyNotExistingHostGroup(KRAFT.getName(), InstanceStatus.CREATED)
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instancesToDelete = distroxUtil.getInstanceIds(testDto, client, KRAFT.getName());
                    cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .await(NODE_FAILURE, RunningParameter.emptyRunningParameter().withoutWaitForFlow())
                .when(distroXTestClient.repair(KRAFT))
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.startKraftMigration())
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.finalizeKraftMigration())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.validateKraftMigrationStatus(KraftMigrationStatus.KRAFT_INSTALLED.name()))
                .validate();
    }

    private enum KraftMigrationStatus {
        ZOOKEEPER_INSTALLED,
        PRE_MIGRATION,
        BROKERS_IN_MIGRATION,
        BROKERS_IN_KRAFT,
        KRAFT_INSTALLED,
        NOT_APPLICABLE
    }
}
