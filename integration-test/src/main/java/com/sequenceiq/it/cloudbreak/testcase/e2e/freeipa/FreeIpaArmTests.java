package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.doNotWaitForFlow;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaAvailabilityAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.FreeIpaImageUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class FreeIpaArmTests extends AbstractE2ETest {

    private static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaAvailabilityAssertion freeIpaAvailabilityAssertion;

    @Inject
    private FreeIpaImageUtil freeIpaImageUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak with arm64 freeipa",
            when = "a valid stack create request is sent with 1 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testSingleArmFreeIpaInstanceUpgrade(TestContext testContext) {
        Pair<Image, Image> armImages = getLastTwoArmImages(testContext);

        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 1)
                .withFreeIpaArchitecture(Architecture.ARM64)
                .withFreeIpaImage(commonCloudProperties().getFreeipaImageCatalogUrl(), armImages.getLeft().getUuid())
                .when(getEnvironmentTestClient().create())
                .given(SdxTestDto.class)
                .withCloudStorage()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .when(freeIpaTestClient.upgrade(commonCloudProperties().getFreeipaImageCatalogUrl(), armImages.getRight().getUuid()))
                .await(Status.UPDATE_IN_PROGRESS, doNotWaitForFlow())
                .given(FreeIpaOperationStatusTestDto.class)
                .then(freeIpaAvailabilityAssertion.availableDuringOperation())
                .await(COMPLETED)
                .given(FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE)
                .then(freeIpaAvailabilityAssertion.available())
                .validate();
    }

    private Pair<Image, Image> getLastTwoArmImages(TestContext testContext) {
        return freeIpaImageUtil.getLastUpgradeableImage(commonCloudProperties().getFreeipaImageCatalogUrl(),
                testContext.getCloudProvider().getCloudPlatform().name(), testContext.getCloudProvider().region(), Architecture.ARM64, false);
    }
}
