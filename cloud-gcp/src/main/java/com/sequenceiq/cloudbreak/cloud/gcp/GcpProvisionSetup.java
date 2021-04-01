package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.buildCompute;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.buildStorage;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getBucket;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getImageName;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getTarName;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Images.Get;
import com.google.api.services.compute.Compute.Images.Insert;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.Image.RawDisk;
import com.google.api.services.compute.model.ImageList;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.Storage.Buckets;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.RewriteResponse;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.polling.GcpImageAttemptMaker;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;

@Service
public class GcpProvisionSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpProvisionSetup.class);

    private static final String READY = "READY";

    private static final int NOT_FOUND = 404;

    private static final int MAX_ATTEMPT_COUNT = 100;

    private static final int SLEEP_TIME_IN_SECONDS = 20;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        try {
            String projectId = getProjectId(credential);
            String imageName = image.getImageName();
            Compute compute = buildCompute(credential);
            ImageList list = compute.images().list(projectId).execute();
            if (!containsSpecificImage(list, imageName)) {
                Storage storage = buildStorage(credential, cloudContext.getName());
                Bucket bucket = createBucketIfNotExists(projectId, authenticatedContext, cloudContext, storage);
                String tarName = getTarName(imageName);
                copyImageTarToBucket(getBucket(imageName), tarName, bucket.getName(), tarName, storage);
                createImageFromTar(projectId, imageName, compute, bucket, tarName);
            }
        } catch (Exception e) {
            String msg = String.format("Error occurred on stack '%s' during the image setup: %s", cloudContext.getName(), e.getMessage());
            LOGGER.error(msg, e);
            throw new CloudConnectorException(msg, e);
        }
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        String projectId = getProjectId(credential);
        String imageName = image.getImageName();
        try {
            Image gcpApiImage = new Image();
            gcpApiImage.setName(getImageName(imageName));
            Compute compute = buildCompute(credential);
            Get getImages = compute.images().get(projectId, gcpApiImage.getName());
            String status = getImages.execute().getStatus();
            LOGGER.info("Status of image {} copy: {}", gcpApiImage.getName(), status);
            if (READY.equals(status)) {
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to retrieve image copy status", e);
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
                if (image.getName().equals(getImageName(imageUrl))) {
                    return true;
                }
            }
        } catch (NullPointerException ignored) {
            return false;
        }
        return false;
    }

    private boolean bucketExist(Storage storage, String bucketName) {
        boolean existingBucket;
        try {
            storage.buckets().get(bucketName).execute();
            existingBucket = true;
        } catch (GoogleJsonResponseException ex) {
            existingBucket = false;
            if (ex.getStatusCode() == NOT_FOUND) {
                LOGGER.warn("Bucket {} does not exist on provider side so we will create it: {}",
                        bucketName, ex.getMessage());
            } else {
                LOGGER.warn("We were not able to get the bucket from Google side with name {} with exception {}. "
                                + "We do not stop the provisioning process because the customer probably doesn't give us storage.get permission.",
                        bucketName, ex.getMessage());
            }
        } catch (Exception e) {
            LOGGER.warn("Unexpected error occurred when we tried to get bucket {} from Google side: {}",
                    bucketName, e.getMessage());
            existingBucket = true;
        }
        return existingBucket;
    }

    private Bucket createBucketIfNotExists(String projectId, AuthenticatedContext authenticatedContext, CloudContext cloudContext, Storage storage) {
        String gcpLocation = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        String bucketName = String.format("%s-%s-%d", projectId, cloudContext.getName(), cloudContext.getId());
        Bucket bucket = new Bucket();
        bucket.setName(bucketName);
        bucket.setStorageClass("STANDARD");
        bucket.setLocation(gcpLocation);
        String errorMessage = String.format("Bucket could not be created with name '%s', in location '%s'", bucketName, gcpLocation);
        try {
            if (!bucketExist(storage, bucketName)) {
                LOGGER.info("Creating bucket with name '{}' in location '{}'", bucketName, gcpLocation);
                Buckets.Insert ins = storage.buckets().insert(projectId, bucket);
                ins.execute();
            } else {
                LOGGER.info("Bucket already exists with name '{}', in location '{}'", bucketName, gcpLocation);
            }
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() != HttpStatus.SC_CONFLICT) {
                LOGGER.error(errorMessage, ex);
                throw new CloudConnectorException(errorMessage, ex);
            } else {
                LOGGER.info("Bucket already exists with name '{}', in location '{}'", bucketName, gcpLocation);
            }
        } catch (IOException ioException) {
            LOGGER.error(errorMessage, ioException);
            throw new CloudConnectorException(errorMessage, ioException);
        }
        return bucket;
    }

    private void copyImageTarToBucket(
            String sourceBucket,
            String sourceKey,
            String destBucket,
            String destKey,
            Storage storage) {
        try {
            Storage.Objects.Rewrite rewrite = storage.objects().rewrite(sourceBucket, sourceKey, destBucket, destKey, new StorageObject());
            RewriteResponse rewriteResponse = rewrite.execute();
            Polling.stopAfterAttempt(MAX_ATTEMPT_COUNT)
                    .stopIfException(true)
                    .waitPeriodly(SLEEP_TIME_IN_SECONDS, TimeUnit.SECONDS)
                    .run(new GcpImageAttemptMaker(rewriteResponse.getRewriteToken(), sourceBucket, sourceKey, destBucket, destKey, storage));
            LOGGER.info("Image copy has been finished successfully for {}/{}.", destBucket, destKey);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Polling exited before timeout. Cause ", userBreakException);
            throw new CloudConnectorException("The image copy failed because the one of the users in your "
                    + "organization stopped the copy process.");
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Poller stopped for image copy: ", pollerStoppedException);
            throw new CloudConnectorException("Image copy failed because the copy has taken too long time. "
                    + "Please check Google Cloud console because probably the image should be ready.");
        } catch (PollerException exception) {
            String message = String.format("Polling failed for image copy to bucket '%s' with key '%s'", destBucket, destKey);
            LOGGER.error(message, exception);
            throw new CloudConnectorException("Image copy failed because: " + exception.getMessage(), exception);
        } catch (Exception e) {
            String message = String.format("Polling could not started to bucket '%s' with key '%s'", destBucket, destKey);
            LOGGER.error(message, e);
            throw new CloudConnectorException("Copying the image could not be started, "
                    + "please check that you are already giving access to Cloudbreak for storage API.", e);
        }
    }

    private void createImageFromTar(String projectId, String imageName, Compute compute, Bucket bucket, String tarName) throws IOException {
        try {
            Image gcpApiImage = new Image();
            gcpApiImage.setName(getImageName(imageName));
            RawDisk rawDisk = new RawDisk();
            rawDisk.setSource(String.format("http://storage.googleapis.com/%s/%s", bucket.getName(), tarName));
            gcpApiImage.setRawDisk(rawDisk);
            Insert ins = compute.images().insert(projectId, gcpApiImage);
            ins.execute();
        } catch (GoogleJsonResponseException googleJsonResponseException) {
            if (googleJsonResponseException.getStatusCode() == HttpStatus.SC_CONFLICT) {
                LOGGER.info("Conflict, the image already exists with name '{}', continuing without error", imageName);
            } else {
                String message = String.format("Failed to create image with name '%s' from tar: '%s' in bucket: '%s'", imageName, tarName, bucket.getName());
                LOGGER.error(message, googleJsonResponseException);
                throw new CloudConnectorException(message, googleJsonResponseException);
            }
        }
    }
}
