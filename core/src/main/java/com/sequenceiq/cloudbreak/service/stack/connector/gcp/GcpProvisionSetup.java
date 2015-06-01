package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.ImageList;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.domain.GcpZone;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class GcpProvisionSetup implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpProvisionSetup.class);

    private static final int CONFLICT = 409;
    private static final int MAX_POLLING_ATTEMPTS = 60;
    private static final int POLLING_INTERVAL = 5000;

    @Autowired
    private GcpStackUtil gcpStackUtil;

    @Autowired
    private PollingService<GcpImageReadyPollerObject> gcpImageReadyPollerObjectPollingService;

    @Autowired
    private GcpImageCheckerStatus gcpImageCheckerStatus;

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) throws Exception {
        PollingResult pollingResult = PollingResult.SUCCESS;
        ProvisionSetupComplete ret = null;
        try {
            Storage storage = gcpStackUtil.buildStorage((GcpCredential) stack.getCredential(), stack);
            Compute compute = gcpStackUtil.buildCompute((GcpCredential) stack.getCredential(), stack);
            GcpCredential credential = (GcpCredential) stack.getCredential();
            ImageList list = compute.images().list(credential.getProjectId()).execute();
            Long time = new Date().getTime();
            if (!containsSpecificImage(list, stack.getImage())) {
                try {
                    Bucket bucket = new Bucket();
                    bucket.setName(credential.getProjectId() + time);
                    bucket.setStorageClass("STANDARD");
                    Storage.Buckets.Insert ins = storage.buckets().insert(credential.getProjectId(), bucket);
                    ins.execute();
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() != CONFLICT) {
                        throw ex;
                    }
                }
                String tarName = gcpStackUtil.getTarName(stack.getImage());
                Storage.Objects.Copy copy = storage.objects().copy(gcpStackUtil.getBucket(stack.getImage()), tarName,
                        credential.getProjectId() + time, tarName,
                        new StorageObject());
                copy.execute();

                Image image = new Image();
                image.setName(gcpStackUtil.getImageName(stack.getImage()));
                Image.RawDisk rawDisk = new Image.RawDisk();
                rawDisk.setSource(String.format("http://storage.googleapis.com/%s/%s", credential.getProjectId() + time, tarName));
                image.setRawDisk(rawDisk);
                Compute.Images.Insert ins1 = compute.images().insert(credential.getProjectId(), image);
                ins1.execute();
                GcpImageReadyPollerObject gcpImageReadyPollerObject = new GcpImageReadyPollerObject(compute,
                        stack, image.getName(), GcpZone.valueOf(stack.getRegion()));
                pollingResult = gcpImageReadyPollerObjectPollingService
                        .pollWithTimeout(gcpImageCheckerStatus, gcpImageReadyPollerObject, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Error occurs on %s stack under the setup", stack.getId()), e);
            throw e;
        }

        if (isSuccess(pollingResult)) {
            ret = new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                    .withSetupProperty(CREDENTIAL, stack.getCredential());
        }
        return ret;
    }

    @Override
    public String preProvisionCheck(Stack stack) {
        return null;
    }

    private boolean containsSpecificImage(ImageList imageList, String imageUrl) {
        try {
            for (Image image : imageList.getItems()) {
                if (image.getName().equals(gcpStackUtil.getImageName(imageUrl))) {
                    return true;
                }
            }
        } catch (NullPointerException ex) {
            return false;
        }
        return false;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    private Map<String, Object> getSetupProperties(Stack stack) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CREDENTIAL, stack.getCredential());
        return properties;
    }

}
