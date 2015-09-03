package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.buildCompute;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.buildStorage;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getBucket;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getImageName;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getTarName;
import static com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults.transformToFalseBooleanResult;

import java.util.Date;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.ImageList;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.BooleanResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.task.GcpImageCheckerTask;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

@Service
public class GcpProvisionSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpProvisionSetup.class);

    @Inject
    private SyncPollingScheduler<BooleanResult> syncPollingScheduler;
    @Inject
    private PollTaskFactory statusCheckFactory;

    @Override
    public void execute(AuthenticatedContext authenticatedContext, CloudStack stack) {
        long stackId = authenticatedContext.getCloudContext().getStackId();
        CloudCredential credential = authenticatedContext.getCloudCredential();
        try {
            String projectId = getProjectId(credential);
            String imageName = stack.getImage().getImageName();
            Storage storage = buildStorage(credential, authenticatedContext.getCloudContext().getStackName());
            Compute compute = buildCompute(credential);
            ImageList list = compute.images().list(projectId).execute();
            Long time = new Date().getTime();
            if (!containsSpecificImage(list, imageName)) {
                try {
                    Bucket bucket = new Bucket();
                    bucket.setName(projectId + time);
                    bucket.setStorageClass("STANDARD");
                    Storage.Buckets.Insert ins = storage.buckets().insert(projectId, bucket);
                    ins.execute();
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() != HttpStatus.SC_CONFLICT) {
                        throw ex;
                    }
                }
                String tarName = getTarName(imageName);
                Storage.Objects.Copy copy = storage.objects().copy(getBucket(imageName), tarName, projectId + time, tarName, new StorageObject());
                copy.execute();

                Image image = new Image();
                image.setName(getImageName(imageName));
                Image.RawDisk rawDisk = new Image.RawDisk();
                rawDisk.setSource(String.format("http://storage.googleapis.com/%s/%s", projectId + time, tarName));
                image.setRawDisk(rawDisk);
                Compute.Images.Insert ins1 = compute.images().insert(projectId, image);
                ins1.execute();
                BooleanResult statePollerResult = transformToFalseBooleanResult(authenticatedContext.getCloudContext());
                PollTask<BooleanResult> task = statusCheckFactory.newPollBooleanStateTask(authenticatedContext,
                        new GcpImageCheckerTask(projectId, image.getName(), compute));
                if (!task.completed(statePollerResult)) {
                    syncPollingScheduler.schedule(task);
                }
            }
        } catch (Exception e) {
            String msg = String.format("Error occurred on %s stack during the setup: %s", stackId, e.getMessage());
            LOGGER.error(msg, e);
            throw new CloudConnectorException(msg, e);
        }
        LOGGER.debug("setup has been executed");
    }

    private boolean containsSpecificImage(ImageList imageList, String imageUrl) {
        try {
            for (Image image : imageList.getItems()) {
                if (image.getName().equals(getImageName(imageUrl))) {
                    return true;
                }
            }
        } catch (NullPointerException ex) {
            return false;
        }
        return false;
    }
}
