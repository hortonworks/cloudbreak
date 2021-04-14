package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getBucket;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getImageName;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getMissingServiceAccountKeyError;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getTarName;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.List;
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
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Images.Get;
import com.google.api.services.compute.Compute.Images.Insert;
import com.google.api.services.compute.model.GuestOsFeature;
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
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpStorageFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
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

    private static final int ATTEMPT_COUNT = 100;

    private static final int SLEEPTIME = 20;

    private static final int NOT_FOUND = 404;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStorageFactory gcpStorageFactory;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String finalImageName = null;
        try {
            String projectId = getProjectId(credential);
            String imageName = image.getImageName();
            Compute compute = gcpComputeFactory.buildCompute(credential);
            ImageList list = compute.images().list(projectId).execute();
            if (!containsSpecificImage(list, imageName)) {
                Storage storage = gcpStorageFactory.buildStorage(credential, cloudContext.getName());
                String accountId = authenticatedContext.getCloudContext().getAccountUUID();
                Bucket bucket = new Bucket();
                String bucketName = GcpLabelUtil.transformLabelKeyOrValue(String.format("%s-%s", accountId, projectId));
                bucket.setName(bucketName);
                bucket.setLocation(authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());
                bucket.setStorageClass("STANDARD");
                try {
                    if (!bucketExist(storage, bucketName)) {
                        Buckets.Insert ins = storage.buckets().insert(projectId, bucket);
                        ins.execute();
                    }
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() != HttpStatus.SC_CONFLICT) {
                        String msg = String.format("Failed to create bucket with name '%s':", bucketName);
                        LOGGER.warn(msg, ex);
                        throw ex;
                    } else {
                        LOGGER.info("No need to create bucket as it exists already with name: {}", bucketName);
                    }
                }
                String tarName = getTarName(imageName);
                copyImage(getBucket(imageName), tarName, bucket.getName(), tarName, storage);

                Image gcpApiImage = new Image();
                finalImageName = getImageName(imageName);
                gcpApiImage.setName(finalImageName);
                RawDisk rawDisk = new RawDisk();
                rawDisk.setSource(String.format("http://storage.googleapis.com/%s/%s", bucket.getName(), tarName));
                gcpApiImage.setRawDisk(rawDisk);
                GuestOsFeature uefiCompatible = new GuestOsFeature().setType("UEFI_COMPATIBLE");
                GuestOsFeature multiIpSubnet = new GuestOsFeature().setType("MULTI_IP_SUBNET");
                gcpApiImage.setGuestOsFeatures(List.of(uefiCompatible, multiIpSubnet));
                try {
                    Insert ins = compute.images().insert(projectId, gcpApiImage);
                    ins.execute();
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() != HttpStatus.SC_CONFLICT) {
                        String detailedMessage = ex.getDetails().getMessage();
                        String msg = String.format("Failed to create image with name '%s' in project '%s': %s", finalImageName, projectId, detailedMessage);
                        LOGGER.warn(msg, ex);
                        throw ex;
                    } else {
                        LOGGER.info("No need to create image as it exists already with name '{}' in project '{}':", finalImageName, projectId);
                    }
                }
            }
        } catch (Exception e) {
            String msg = String.format("Error occurred on %s stack during the image creation process%s: %s", cloudContext.getName(),
                    isBlank(finalImageName) ? "" : ", image name: " + finalImageName,
                    e.getMessage());
            LOGGER.warn(msg, e);
            throw new CloudConnectorException(msg, e);
        }
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
                                + "We do not stop the provisioning process because the customer probably dont give us storage.get permission.",
                        bucketName, ex.getMessage());
            }
        } catch (Exception e) {
            LOGGER.warn("Unexpected error occurred when we tried to get bucket {} from Google side: {}",
                    bucketName, e.getMessage());
            existingBucket = true;
        }
        return existingBucket;
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
            Polling.stopAfterAttempt(ATTEMPT_COUNT)
                    .stopIfException(true)
                    .waitPeriodly(SLEEPTIME, TimeUnit.SECONDS)
                    .run(new GcpImageAttemptMaker(rewriteResponse.getRewriteToken(), sourceBucket, sourceKey, destBucket, destKey, storage));
            LOGGER.info("Image copy has been finished successfully for {}/{}.", destBucket, destKey);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Polling exited before timeout. Cause ", userBreakException);
            throw new CloudbreakServiceException("The image copy failed because the one of the user in your "
                    + "organization stopped the copy process.");
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
        String projectId = getProjectId(credential);
        String imageName = image.getImageName();
        try {
            Image gcpApiImage = new Image();
            gcpApiImage.setName(getImageName(imageName));
            Compute compute = gcpComputeFactory.buildCompute(credential);
            Get getImages = compute.images().get(projectId, gcpApiImage.getName());
            String status = getImages.execute().getStatus();
            LOGGER.debug("Status of image {} copy: {}", gcpApiImage.getName(), status);
            if (READY.equals(status)) {
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            }
        } catch (TokenResponseException e) {
            getMissingServiceAccountKeyError(e, projectId);
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
                if (image.getName().equals(getImageName(imageUrl))) {
                    return true;
                }
            }
        } catch (NullPointerException ignored) {
            return false;
        }
        return false;
    }
}
