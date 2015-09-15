package com.sequenceiq.cloudbreak.cloud.arm.poller;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmTemplateUtils.NOT_FOUND;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.ImageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import groovyx.net.http.HttpResponseException;

public class ArmImageCopyStatusCheckerTask implements BooleanStateConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmImageCopyStatusCheckerTask.class);

    private ImageCheckerContext imageCheckerContext;
    private ArmClient armClient;

    public ArmImageCopyStatusCheckerTask(ArmClient armClient, ImageCheckerContext imageCheckerContext) {
        this.imageCheckerContext = imageCheckerContext;
        this.armClient = armClient;
    }

    @Override
    public Boolean check(AuthenticatedContext authenticatedContext) {
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
