package com.sequenceiq.it.cloudbreak.assertion.image;

import static com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest.STACK_CREATE_IN_PROGRESS;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1EventEndpoint;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEventEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class ImageAssertions {

    private static final int IMAGE_WAIT_SLEEP_TIME_IN_SECONDS = 10;

    private static final int IMAGE_SETUP_SLEEP_TIME_IN_SECONDS = 10;

    @Value("${integrationtest.imageValidation.imageWait.timeoutInMinutes:15}")
    private int imageWaitTimeoutInMinutes;

    @Value("${integrationtest.imageValidation.imagesetup.timeoutInMinutes:60}")
    private int imageSetupTimeoutInMinutes;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    public Assertion<ImageCatalogTestDto, CloudbreakClient> validateContainsImage(String imageUuid) {
        return (testContext, testDto, client) -> {
            try {
                Polling.waitPeriodly(IMAGE_WAIT_SLEEP_TIME_IN_SECONDS, TimeUnit.SECONDS)
                        .stopAfterDelay(imageWaitTimeoutInMinutes, TimeUnit.MINUTES)
                        .stopIfException(true)
                        .run(() -> containsImageAttempt(testDto, imageUuid));
            } catch (PollerStoppedException e) {
                String message = String.format("%s image is missing from the '%s' catalog.", imageUuid, testContext.get(ImageCatalogTestDto.class).getName());
                throw new TestFailException(message, e);
            }
            return testDto;
        };
    }

    private AttemptResult<ImageV4Response> containsImageAttempt(ImageCatalogTestDto testDto, String imageUuid) {
        testDto
                .when(imageCatalogTestClient.getV4WithAllImages())
                .validate();
        ImagesV4Response imagesV4Response = testDto.getResponse().getImages();
        List<ImageV4Response> images = new LinkedList<>();
        images.addAll(imagesV4Response.getBaseImages());
        images.addAll(imagesV4Response.getCdhImages());
        Optional<ImageV4Response> image = images.stream()
                .filter(img -> img.getUuid().equalsIgnoreCase(imageUuid))
                .findFirst();
        return image.isPresent()
                ? AttemptResults.finishWith(image.get())
                : AttemptResults.justContinue();
    }

    public Assertion<SdxInternalTestDto, SdxClient> validateSdxInternalImageSetupTime() {
        return (testContext, testDto, client) -> {
            testDto
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, RunningParameter.emptyRunningParameter().withoutWaitForFlow())
                .validate();
            AttemptMaker<Boolean> attemptMaker = getImageSetupAttemptMaker(testDto, client);
            return awaitImageSetp(testDto, attemptMaker);
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> validateDistroXImageSetupTime() {
        return (testContext, testDto, client) -> {
            testDto
                    .await(STACK_CREATE_IN_PROGRESS, RunningParameter.emptyRunningParameter().withoutWaitForFlow())
                    .validate();
            AttemptMaker<Boolean> attemptMaker = getImageSetupAttemptMaker(testDto, client);
            return awaitImageSetp(testDto, attemptMaker);
        };
    }

    private AttemptMaker<Boolean> getImageSetupAttemptMaker(SdxInternalTestDto testDto, SdxClient client) {
        SdxEndpoint sdxEndpoint = client.getDefaultClient(testDto.getTestContext()).sdxEndpoint();
        SdxEventEndpoint sdxEventEndpoint = client.getDefaultClient(testDto.getTestContext()).sdxEventEndpoint();
        String environmentCrn = testDto.getResponse().getEnvironmentCrn();
        List<StructuredEventType> eventTypes = List.of(StructuredEventType.NOTIFICATION);
        return () -> {
            SdxClusterResponse sdxClusterResponse = sdxEndpoint.get(testDto.getName());
            if (SdxClusterStatusResponse.PROVISIONING_FAILED.equals(sdxClusterResponse.getStatus())) {
                // provisioning failed, do not also fail for image setup
                return AttemptResults.finishWith(true);
            }
            List<CDPStructuredEvent> events = sdxEventEndpoint.getAuditEvents(environmentCrn, eventTypes, null, null);
            boolean imageSetupFinished = events.size() > 1 && events.subList(1, events.size()).stream()
                    .anyMatch(auditEvent -> "Setting up CDP image".equals(auditEvent.getStatusReason()));
            if (imageSetupFinished) {
                return AttemptResults.finishWith(true);
            }
            return AttemptResults.justContinue();
        };
    }

    private AttemptMaker<Boolean> getImageSetupAttemptMaker(DistroXTestDto testDto, CloudbreakClient client) {
        DistroXV1Endpoint distroXV1Endpoint = client.getDefaultClient(testDto.getTestContext()).distroXV1Endpoint();
        DistroXV1EventEndpoint eventEndpoint = client.getDefaultClient(testDto.getTestContext()).distroXV1EventEndpoint();
        String environmentCrn = testDto.getResponse().getEnvironmentCrn();
        List<StructuredEventType> eventTypes = List.of(StructuredEventType.NOTIFICATION);
        return () -> {
            StackV4Response stackV4Response = distroXV1Endpoint.getByCrn(testDto.getCrn(), null);
            if (Status.CREATE_FAILED.equals(stackV4Response.getStatus())) {
                // provisioning failed, do not also fail for image setup
                return AttemptResults.finishWith(true);
            }
            List<CDPStructuredEvent> events = eventEndpoint.getAuditEvents(testDto.getCrn(), null, null);
            boolean imageSetupFinished = events.size() > 1 && events.subList(1, events.size()).stream()
                    .anyMatch(event -> "Setting up CDP image".equals(event.getStatusReason()));
            if (imageSetupFinished) {
                return AttemptResults.finishWith(true);
            }
            return AttemptResults.justContinue();
        };
    }

    private <T extends CloudbreakTestDto> T awaitImageSetp(T testDto, AttemptMaker<Boolean> attemptMaker) {
        try {
            Polling.waitPeriodly(IMAGE_SETUP_SLEEP_TIME_IN_SECONDS, TimeUnit.SECONDS)
                    .stopAfterDelay(imageSetupTimeoutInMinutes, TimeUnit.MINUTES)
                    .stopIfException(true)
                    .run(attemptMaker);
        } catch (PollerStoppedException e) {
            throw new TestFailException(String.format("Image setup exceeded %d minutes", imageSetupTimeoutInMinutes), e);
        }
        return testDto;
    }

}
