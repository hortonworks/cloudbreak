package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COORDINATOR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.EXECUTOR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto.getHostGroupMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXGcpVolumesTest extends AbstractE2ETest {
    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private GcpProperties gcpProperties;

    @Inject
    private DistroXVolumeValidationService distroXVolumeValidationService;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
        createEnvironment(testContext, Boolean.FALSE, 1);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak and an environment with no freeipa",
            when = "creating a new FreeIpa, Datalake and Datahub with instancetypes supporting hyperdisk on GCP",
            then = "Freeipa, Datalake, Datahub should be created successfuly, all the datahub disks should be properly mounted")
    public void testNewGenGcpInstanceTypesWithHyperDiskSupport(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .withInstanceType(gcpProperties.getInstance().getHyperDiskInstanceType())
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .validate();

        testContext
                .given(SdxTestDto.class)
                .withCustomInstanceGroup("master", gcpProperties.getInstance().getHyperDiskInstanceType())
                .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();

        testContext
                .given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataMartDistroXBlueprintNameForCurrentRuntime())
                .withInstanceGroupsEntity(setupCustomHostGroups(testContext))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.get())
                .then((tc, testDto, client) -> distroXVolumeValidationService.validateAttachedDisks(testDto, tc, client))
                .validate();
    }

    private Collection<DistroXInstanceGroupTestDto> setupCustomHostGroups(TestContext testContext) {
        Map<HostGroupType, DistroXInstanceGroupTestDto> hostGroups = getHostGroupMap(testContext, testContext.getCloudPlatform(), MASTER, COORDINATOR, EXECUTOR);
        DistroXInstanceGroupTestDto master = hostGroups.get(MASTER);
        VolumeV4TestDto attachedVolumes = testContext.init(VolumeV4TestDto.class, testContext.getCloudPlatform());
        DistroXInstanceTemplateTestDto masterTemplate = testContext.init(DistroXInstanceTemplateTestDto.class, testContext.getCloudPlatform());
        VolumeV1Request hyperDiskBalanced = new VolumeV1Request();
        hyperDiskBalanced.setCount(2);
        hyperDiskBalanced.setSize(attachedVolumes.getRequest().getSize());
        hyperDiskBalanced.setType(gcpProperties.getInstance().getHyperDiskBalanced());
        VolumeV1Request hyperDiskThroughput = new VolumeV1Request();
        hyperDiskThroughput.setCount(1);
        hyperDiskThroughput.setSize(2048);
        hyperDiskThroughput.setType(gcpProperties.getInstance().getHyperDiskThroughput());
        masterTemplate.getRequest().setAttachedVolumes(Set.of(hyperDiskBalanced, hyperDiskThroughput));
        masterTemplate.getRequest().setInstanceType(gcpProperties.getInstance().getHyperDiskInstanceType());
        master.withTemplate(masterTemplate);

        DistroXInstanceGroupTestDto executor = hostGroups.get(EXECUTOR);
        DistroXInstanceTemplateTestDto executorTemplate = testContext.init(DistroXInstanceTemplateTestDto.class, testContext.getCloudPlatform());
        VolumeV1Request pdBalanced = new VolumeV1Request();
        pdBalanced.setCount(2);
        pdBalanced.setSize(attachedVolumes.getRequest().getSize());
        pdBalanced.setType(gcpProperties.getInstance().getVolumeType());
        executorTemplate.getRequest().setAttachedVolumes(Set.of(pdBalanced, hyperDiskBalanced));
        executorTemplate.getRequest().setInstanceType(gcpProperties.getInstance().getMixedInstanceType());
        executor.withTemplate(executorTemplate);
        return hostGroups.values();
    }
}
