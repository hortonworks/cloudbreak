package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEventEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class PrewarmImageValidatorE2ETest extends AbstractE2ETest implements ImageValidatorE2ETest {

    private static final int IMAGE_SETUP_SLEEP_TIME_IN_SECONDS = 10;

    @Value("${integrationtest.imageValidation.imagesetup.timeoutInMinutes:60}")
    private int imageSetupTimeoutInMinutes;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "a SDX internal request and a DistroX request",
            when = "a SDX internal create request is sent",
            then = "the SDX cluster and the corresponding DistroX cluster is created")
    public void testCreateInternalSdxAndDistrox(TestContext testContext) {
        testContext.given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs();
        testContext.given(SdxInternalTestDto.class)
                .withoutDatabase()
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withTemplate(commonClusterManagerProperties().getInternalSdxBlueprintName())
                .withImageCatalogNameAndImageId(commonCloudProperties().getImageValidation().getSourceCatalogName(),
                        commonCloudProperties().getImageValidation().getImageUuid())
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, RunningParameter.emptyRunningParameter().withoutWaitForFlow())
                .then((tc, testDto, client) -> {
                    try {
                        Polling.waitPeriodly(IMAGE_SETUP_SLEEP_TIME_IN_SECONDS, TimeUnit.SECONDS)
                                .stopAfterDelay(imageSetupTimeoutInMinutes, TimeUnit.MINUTES)
                                .stopIfException(true)
                                .run(imageSetupResultAttemptMaker(testDto, client));
                    } catch (PollerStoppedException e) {
                        throw new TestFailException(String.format("Image setup exceeded %d minutes", imageSetupTimeoutInMinutes), e);
                    }
                    return testDto;
                })
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.describeInternal())
                .validate();
        testContext.given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintNameForCurrentRuntime())
                .withImageSettings(testContext
                        .given(DistroXImageTestDto.class)
                        .withImageCatalog(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getCatalogName())
                        .withImageId(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId()))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .when(distroXTestClient.get())
                .validate();
        testContext.given(SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternal())
                .then(((context, sdx, client) -> {
                    SdxClusterStatusResponse clusterStatus = sdx.getResponse().getStatus();
                    if (!clusterStatus.isRunning()) {
                        throw new TestFailException("SDX status is not running at the end of the validation. Current status is: " + clusterStatus);
                    }
                    return sdx;
                }))
                .validate();
    }

    private AttemptMaker<Boolean> imageSetupResultAttemptMaker(SdxInternalTestDto testDto, SdxClient client) {
        SdxEndpoint sdxEndpoint = client.getDefaultClient().sdxEndpoint();
        SdxEventEndpoint sdxEventEndpoint = client.getDefaultClient().sdxEventEndpoint();
        String environmentCrn = testDto.getResponse().getEnvironmentCrn();
        List<StructuredEventType> eventTypes = List.of(StructuredEventType.NOTIFICATION);
        return () -> {
            SdxClusterResponse sdxClusterResponse = sdxEndpoint.get(testDto.getName());
            if (SdxClusterStatusResponse.PROVISIONING_FAILED.equals(sdxClusterResponse.getStatus())) {
                // provisioning failed, do not also fail for image setup
                return AttemptResults.finishWith(true);
            }
            List<CDPStructuredEvent> auditEvents = sdxEventEndpoint.getAuditEvents(environmentCrn, eventTypes, null, null);
            boolean imageSetupFinished = auditEvents.subList(1, auditEvents.size()).stream()
                    .anyMatch(auditEvent -> "Setting up CDP image".equals(auditEvent.getStatusReason()));
            if (imageSetupFinished) {
                return AttemptResults.finishWith(true);
            }
            return AttemptResults.justContinue();
        };
    }
}
