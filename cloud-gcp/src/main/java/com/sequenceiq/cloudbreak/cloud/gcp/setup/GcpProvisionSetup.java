package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Images.Get;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.ImageList;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.RewriteResponse;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpStorageFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;

@Service
public class GcpProvisionSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpProvisionSetup.class);

    private static final String READY = "READY";

    private static final int ATTEMPT_COUNT = 300;

    private static final int SLEEPTIME = 20;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpImageRegisterService gcpImageRegisterService;

    @Inject
    private GcpBucketRegisterService gcpBucketRegisterService;

    @Inject
    private GcpStorageFactory gcpStorageFactory;

    @Inject
    private GcpImageAttemptMakerFactory gcpImageAttemptMakerFactory;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String finalImageName = null;
        try {
            String projectId = gcpStackUtil.getProjectId(credential);
            String imageName = image.getImageName();
            Compute compute = gcpComputeFactory.buildCompute(credential);
            ImageList imageList = compute.images().list(projectId).execute();
            if (!containsSpecificImage(imageList, imageName)) {
                Storage storage = gcpStorageFactory.buildStorage(credential, cloudContext.getName());
                String bucketName = gcpBucketRegisterService.register(authenticatedContext);
                String tarName = gcpStackUtil.getTarName(imageName);
                copyImage(gcpStackUtil.getBucket(imageName), tarName, bucketName, tarName, storage);
                gcpImageRegisterService.register(authenticatedContext, bucketName, imageName);
            }
        } catch (Exception e) {
            String msg = String.format("Error occurred on %s stack during the image creation process%s: %s", cloudContext.getName(),
                    isBlank(finalImageName) ? "" : ", image name: " + finalImageName,
                    e.getMessage());
            LOGGER.warn(msg, e);
            throw new CloudConnectorException(msg, e);
        }
    }

    public void copyImage(
            final String sourceBucket,
            final String sourceKey,
            final String destBucket,
            final String destKey,
            Storage storage) {
        try {
            Storage.Objects.Rewrite rewrite = storage.objects().rewrite(sourceBucket, sourceKey, destBucket, destKey, new StorageObject());
            RewriteResponse rewriteResponse = rewrite.execute();
            GcpImageAttemptMaker gcpImageAttemptMaker = gcpImageAttemptMakerFactory.create(
                    rewriteResponse.getRewriteToken(),
                    sourceBucket,
                    sourceKey,
                    destBucket,
                    destKey,
                    storage
            );
            Polling.stopAfterAttempt(ATTEMPT_COUNT)
                    .stopIfException(true)
                    .waitPeriodly(SLEEPTIME, TimeUnit.SECONDS)
                    .run(gcpImageAttemptMaker);
            LOGGER.info("Image copy has been finished successfully for {}/{}.", destBucket, destKey);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Poller stopped for image copy: ", pollerStoppedException);
            throw new CloudbreakServiceException("Image copy failed because the copy take too long time. "
                    + "Please check Google Cloud console because probably the image should be ready.");
        } catch (PollerException exception) {
            LOGGER.error("Polling failed for image copy: {}", sourceKey, exception);
            throw new CloudbreakServiceException("Image copy failed because: " + exception.getMessage());
        } catch (Exception e) {
            LOGGER.error("Polling could not started because: {}", e.getMessage(), e);
            throw new CloudbreakServiceException("Copying the image could not be started, "
                    + "please check whether you have given access to CDP for storage API.");
        }
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        String projectId = gcpStackUtil.getProjectId(credential);
        String imageName = image.getImageName();
        try {
            Image gcpApiImage = new Image();
            gcpApiImage.setName(gcpStackUtil.getImageName(imageName));
            Compute compute = gcpComputeFactory.buildCompute(credential);
            Get getImages = compute.images().get(projectId, gcpApiImage.getName());
            String status = getImages.execute().getStatus();
            LOGGER.debug("Status of image {} copy: {}", gcpApiImage.getName(), status);
            if (READY.equals(status)) {
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            }
        } catch (TokenResponseException e) {
            gcpStackUtil.getMissingServiceAccountKeyError(e, projectId);
        } catch (IOException e) {
            LOGGER.info("Failed to retrieve image copy status", e);
            return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
        }
        return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
    }

    @Override
    public void prerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        LOGGER.debug("setup has been executed");
    }

    @Override
    public void validateFileSystem(CloudCredential credential, SpiFileSystem spiFileSystem) {
    }

    @Override
    public void validateParameters(AuthenticatedContext authenticatedContext, Map<String, String> parameters) {

    }

    @Override
    public void scalingPrerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, boolean upscale) {

    }

    private boolean containsSpecificImage(ImageList imageList, String imageUrl) {
        try {
            for (Image image : imageList.getItems()) {
                if (image.getName().equals(gcpStackUtil.getImageName(imageUrl))) {
                    return true;
                }
            }
        } catch (NullPointerException ignored) {
            return false;
        }
        return false;
    }
}
