package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpStorageFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class GcpBucketRegisterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpProvisionSetup.class);

    private static final int NOT_FOUND = 404;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpStorageFactory gcpStorageFactory;

    public String register(AuthenticatedContext authenticatedContext) throws IOException {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        String projectId = gcpStackUtil.getProjectId(credential);
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String accountId = authenticatedContext.getCloudContext().getAccountId();
        Storage storage = gcpStorageFactory.buildStorage(credential, cloudContext.getName());
        Bucket bucket = new Bucket();
        String bucketName = gcpLabelUtil.transformLabelKeyOrValue(String.format("%s-%s", accountId, projectId));
        bucket.setName(bucketName);
        bucket.setLocation(authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());
        bucket.setStorageClass("STANDARD");
        try {
            if (!bucketExist(storage, bucketName)) {
                Storage.Buckets.Insert ins = storage.buckets().insert(projectId, bucket);
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
        return bucketName;
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

}