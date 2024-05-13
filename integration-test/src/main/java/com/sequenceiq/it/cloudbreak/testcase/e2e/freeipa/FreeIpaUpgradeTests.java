package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.doNotWaitForFlow;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaAvailabilityAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class FreeIpaUpgradeTests extends AbstractE2ETest implements ImageValidatorE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaAvailabilityAssertion freeIpaAvailabilityAssertion;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 1 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testSingleFreeIpaInstanceUpgrade(TestContext testContext) {
        testFreeIpaUpgrade(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 3 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testHAFreeIpaInstanceUpgrade(TestContext testContext) {
        testContext.given(FreeIpaTestDto.class).withFreeIpaHa(1, 3);
        testFreeIpaUpgrade(testContext);
    }

    private void testFreeIpaUpgrade(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                    .withTelemetry("telemetry")
                    .withUpgradeCatalogAndImage()
                .when(freeIpaTestClient.create())
                .await(FREEIPA_AVAILABLE)
                .when(freeIpaTestClient.describe())
                .given(SdxTestDto.class)
                    .withCloudStorage()
                    .withoutExternalDatabase()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.upgrade(imageValidatorE2ETestUtil.getImageCatalogName(), imageValidatorE2ETestUtil.getImageUuid()))
                .await(Status.UPDATE_IN_PROGRESS, doNotWaitForFlow())
                .given(FreeIpaOperationStatusTestDto.class)
                .then(freeIpaAvailabilityAssertion.availableDuringOperation())
                .await(COMPLETED)
                .given(FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE)
                .then(freeIpaAvailabilityAssertion.available())
                .validate();
    }
}
