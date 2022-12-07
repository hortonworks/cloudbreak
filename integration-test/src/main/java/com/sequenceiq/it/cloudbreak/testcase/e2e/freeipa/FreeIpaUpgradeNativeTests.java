package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class FreeIpaUpgradeNativeTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    private static final long FIVE_MINUTES_IN_SEC = 5L * 60;

    private static final String CHECK_DNS_LOOKUPS_CMD = "ping -c 2 %s | grep -q '%s'";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private AwsCloudFunctionality cloudFunctionality;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak, and freeipa with cloudformation",
            when = "upgrade ",
            then = "migration happens into native")
    public void testSingleFreeIpaNativeUpgrade(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setCreate(false);

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .withVariant("AWS")
                .withUpgradeCatalogAndImage()
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .then(freeIpaCloudFromationStackDoesExist())
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .given(freeIpa, FreeIpaTestDto.class)
                .when(freeIpaTestClient.upgrade())
                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .then(freeIpaCloudFromationStackDoesNotExist())
                .validate();
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> freeIpaCloudFromationStackDoesExist() {
        return isFreeIpaCloudFromationStackDoesExist(true);
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> freeIpaCloudFromationStackDoesNotExist() {
        return isFreeIpaCloudFromationStackDoesExist(false);
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> isFreeIpaCloudFromationStackDoesExist(boolean exist) {
        return (tc, freeipaTestDto, client) -> {
            Boolean res = cloudFunctionality.isFreeipaCfStackExistForEnvironment(freeipaTestDto.getEnvironmentCrn());
            org.assertj.core.api.Assertions.assertThat(res).as("freeipa cloudformation template for environment should "
                    + (exist  ? "exist." : "not exist.")).isEqualTo(exist);

            return freeipaTestDto;
        };
    }
}