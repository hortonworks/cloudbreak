package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.doNotWaitForFlow;

import java.util.Optional;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
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

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 1 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testSingleFreeIpaInstanceUpgrade(TestContext testContext) {
        testFreeIpaUpgrade(testContext, 1);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 3 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testHAFreeIpaInstanceUpgrade(TestContext testContext) {
        testFreeIpaUpgrade(testContext, 3);
    }

    private void testFreeIpaUpgrade(TestContext testContext, int freeIpaInstances) {
        setUpEnvironmentTestDto(testContext, Boolean.TRUE, freeIpaInstances, Optional.of(FreeIpaLoadBalancerType.NONE))
                .withFreeIpaImage(testContext.getCloudProvider().getFreeIpaUpgradeImageCatalog(), testContext.getCloudProvider().getFreeIpaUpgradeImageId())
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(SdxTestDto.class)
                    .withCloudStorage()
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
                .then(freeIpaAvailabilityAssertion.availableLoadBalancer())
                .validate();
    }

    @Override
    public String getCbImageId(TestContext testContext) {
        return testContext.get(SdxTestDto.class).getResponse().getStackV4Response().getImage().getId();
    }
}
