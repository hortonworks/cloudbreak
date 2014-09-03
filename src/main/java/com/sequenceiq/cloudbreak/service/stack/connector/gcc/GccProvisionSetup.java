package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.ImageList;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class GccProvisionSetup implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccProvisionSetup.class);

    private static final String BUCKET_NAME = "sequenceiqimage";
    private static final String IMAGE_NAME = "debian-docker-ambari";
    private static final String MAIN_PROJECT = "siq-haas";
    private static final String TAR_NAME = "ubuntu_14.04_image.tar.gz";

    @Autowired
    private Reactor reactor;

    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public void setupProvisioning(Stack stack) {
        try {
            Storage storage = gccStackUtil.buildStorage((GccCredential) stack.getCredential(), stack);
            Compute compute = gccStackUtil.buildCompute((GccCredential) stack.getCredential(), stack);
            GccTemplate template = (GccTemplate) stack.getTemplate();

            ImageList list = compute.images().list(template.getProjectId()).execute();
            if (!containsSpecificImage(list)) {
                Bucket bucket = new Bucket();
                bucket.setName(BUCKET_NAME);
                bucket.setStorageClass("STANDARD");
                Storage.Buckets.Insert ins = storage.buckets().insert(template.getProjectId(), bucket);
                ins.execute();
                Storage.Objects.Copy copy = storage.objects().copy(MAIN_PROJECT, TAR_NAME, BUCKET_NAME, TAR_NAME, new StorageObject());
                copy.execute();

                Image image = new Image();
                image.setName(IMAGE_NAME);
                Image.RawDisk rawDisk = new Image.RawDisk();
                rawDisk.setSource(String.format("http://storage.googleapis.com/%s/%s", BUCKET_NAME, TAR_NAME));
                image.setRawDisk(rawDisk);
                Compute.Images.Insert ins1 = compute.images().insert(template.getProjectId(), image);
                ins1.execute();
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Error occurs on %s stack under the setup", stack.getId()), e);
            throw new InternalServerException(e.getMessage());
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT,
                Event.wrap(
                        new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                                .withSetupProperty(CREDENTIAL, stack.getCredential())
                )
        );
    }

    private boolean containsSpecificImage(ImageList imageList) {
        for (Image image : imageList.getItems()) {
            if (image.getName().equals(IMAGE_NAME)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }

    @Override
    public Map<String, Object> getSetupProperties(Stack stack) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CREDENTIAL, stack.getCredential());
        return properties;    }

    @Override
    public Map<String, String> getUserDataProperties(Stack stack) {
        return new HashMap<>();
    }
}
