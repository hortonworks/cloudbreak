package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXVolumesVerticalScaleTest extends AbstractE2ETest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String TEST_INSTANCE_GROUP = "compute";

    private static final int UPDATE_SIZE = 500;

    private static final String UPDATE_DISK_TYPE = "gp3";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datahub",
            when = "disk resize is called first then delete disks is called on the Datahubs",
            then = "attached EBS volumes on datahubs must be modified to the new type and size" +
                    " and then deleted, the new datahub should be up and running"
    )
    public void testDistroXVolumesVerticalScale(TestContext testContext) {
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
            .given("telemetry", TelemetryTestDto.class)
            .withLogging()
            .withReportClusterLogs()
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .addTags(SDX_TAGS)
            .withoutDatabase()
            .withCloudStorage(getCloudStorageRequest(testContext))
            .when(sdxTestClient.createInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                    .defaultHostGroup()
                    .withStorageOptimizedInstancetype()
                    .build())
            .addTags(DX_TAGS)
            .when(distroXTestClient.create(), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.updateDisks(UPDATE_SIZE, UPDATE_DISK_TYPE, TEST_INSTANCE_GROUP), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.get(), RunningParameter.key("dx"))
            .then((tc, testDto, client) -> {
                validateVerticalScale(testDto, tc, client);
                return testDto;
            })
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.deleteDisks(), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .validate();
    }

    private void validateVerticalScale(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client) {
        StackV4Response stackV4Response = distroXTestDto.getResponse();
        InstanceGroupV4Response instanceGroup = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals(TEST_INSTANCE_GROUP))
                .findFirst().orElseThrow();
        List<String> updatedInstances = distroxUtil.getInstanceIds(distroXTestDto, client, TEST_INSTANCE_GROUP);
        CloudFunctionality cloudFunctionality = getCloudFunctionality(tc);
        List<String> attachedVolumes = cloudFunctionality.listInstancesVolumeIds(distroXTestDto.getName(), updatedInstances);
        List<Volume> attachedVolumesAttributes = cloudFunctionality.describeVolumes(attachedVolumes);
        instanceGroup.getTemplate().getAttachedVolumes().forEach(vol -> {
            if (vol.getSize() != UPDATE_SIZE || !UPDATE_DISK_TYPE.equals(vol.getType().toLowerCase(Locale.ROOT))) {
                throw new TestFailException(String.format("Disk Update did not complete successfully for instances in group %s",
                        TEST_INSTANCE_GROUP));
            }
        });
        attachedVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != UPDATE_SIZE || !UPDATE_DISK_TYPE.equals(vol.getType().toLowerCase(Locale.ROOT))) {
                throw new TestFailException(String.format("Disk Update did not complete successfully for instances on cloud provider in group %s",
                        TEST_INSTANCE_GROUP));
            }
        });
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}
