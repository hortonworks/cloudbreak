package com.sequenceiq.cloudbreak.cloud.arm.task;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmTemplateUtils.NOT_FOUND;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.ImageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import groovyx.net.http.HttpResponseException;

public class ArmImageCopyStatusCheckerTask extends PollBooleanStateTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmImageCopyStatusCheckerTask.class);

    private ImageCheckerContext imageCheckerContext;
    private ArmClient armClient;

    public ArmImageCopyStatusCheckerTask(AuthenticatedContext ac, ArmClient armClient, ImageCheckerContext imageCheckerContext) {
        super(ac, true);
        this.imageCheckerContext = imageCheckerContext;
        this.armClient = armClient;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.createAccess(imageCheckerContext.getArmCredentialView());
        try {

            CopyState copyState = client.getCopyStatus(imageCheckerContext.getGroupName(), imageCheckerContext.getStorageName(),
                    imageCheckerContext.getContainerName(), imageCheckerContext.getSourceBlob());
            if (CopyStatus.SUCCESS.equals(copyState.getStatus())) {
                return true;
            } else if (CopyStatus.ABORTED.equals(copyState.getStatus()) || CopyStatus.INVALID.equals(copyState.getStatus())) {
                throw new CloudConnectorException(copyState.getStatusDescription());
            } else {
                LOGGER.info(String.format("CopyStatus Pending %s byte/%s byte: %s",
                        copyState.getTotalBytes(), copyState.getBytesCopied(), copyState.getStatusDescription()));
            }
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != NOT_FOUND) {
                throw new CloudConnectorException(e.getResponse().getData().toString());
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

}
