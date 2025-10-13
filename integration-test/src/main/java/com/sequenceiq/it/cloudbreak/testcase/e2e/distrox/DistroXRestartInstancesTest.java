package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.RestartTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.restart.RestartInstancesTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXRestartInstancesTest extends AbstractE2ETest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String INSTANCE_GROUP = "executor";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private RestartTestClient restartTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

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
            when = "restart Instances is called",
            then = "Instances are restarted and datahub is running"
    )
    public void testDistroXRestartInstances(TestContext testContext) {
        AtomicReference<String> datahubName = new AtomicReference<>();
        AtomicReference<String> datahubCrn = new AtomicReference<>();
        List<String> dataHubInstancesToRestart = new ArrayList<>();

        testContext
            .given("telemetry", TelemetryTestDto.class)
            .withLogging()
            .withReportClusterLogs()
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .addTags(SDX_TAGS)
            .withCloudStorage(getCloudStorageRequest(testContext))
            .when(sdxTestClient.createInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .withTemplate(commonClusterManagerProperties.getDataMartDistroXBlueprintNameForCurrentRuntime())
            .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataMartHostGroups(testContext, testContext.getCloudPlatform()))
            .addTags(DX_TAGS)
            .when(distroXTestClient.create(), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .then((tc, testDto, client) -> {
                datahubName.set(testDto.getName());
                datahubCrn.set(testDto.getCrn());
                dataHubInstancesToRestart.addAll(distroxUtil.getInstanceIds(testDto, client, INSTANCE_GROUP));
                return testDto;
            })
            .given("restartDistrox", RestartInstancesTestDto.class)
            .withName(datahubName.get())
            .withCrn(datahubCrn.get())
            .when(restartTestClient.restartInstances(dataHubInstancesToRestart))
            .awaitForFlow()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.get(), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .validate();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}
