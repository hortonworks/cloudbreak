package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.RestartTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.restart.RestartInstancesTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRestartInstancesTest extends AbstractE2ETest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final String INSTANCE_GROUP = "idbroker";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RestartTestClient restartTestClient;

    @Inject
    private SdxUtil sdxUtil;

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
            given = "there is an available environment with a running datalake",
            when = "restart Instances is called",
            then = "Instances are restarted and datalake is running"
    )
    public void testSdxRestartInstances(TestContext testContext) {
        AtomicReference<String> dataLakeName = new AtomicReference<>();
        AtomicReference<String> dataLakeCrn = new AtomicReference<>();
        List<String> dataLakeInstancesToRestart = new ArrayList<>();

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
            .then((tc, testDto, client) -> {
                    dataLakeName.set(testDto.getName());
                    dataLakeCrn.set(testDto.getCrn());
                    dataLakeInstancesToRestart.addAll(sdxUtil.getInstanceIds(testDto, client, INSTANCE_GROUP));
                    return testDto;
            })
            .given("restartDistrox", RestartInstancesTestDto.class)
            .withName(dataLakeName.get())
            .withCrn(dataLakeCrn.get())
            .when(restartTestClient.restartInstances(dataLakeInstancesToRestart))
            .awaitForFlow()
            .given(SdxInternalTestDto.class)
            .when(sdxTestClient.describeInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .validate();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}
