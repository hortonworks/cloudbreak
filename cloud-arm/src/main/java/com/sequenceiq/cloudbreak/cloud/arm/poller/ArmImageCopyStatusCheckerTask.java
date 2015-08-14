package com.sequenceiq.cloudbreak.cloud.arm.poller;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmTemplateUtils.NOT_FOUND;

import com.microsoft.azure.storage.blob.LeaseStatus;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.ImageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import groovyx.net.http.HttpResponseException;

public class ArmImageCopyStatusCheckerTask implements BooleanStateConnector {

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
            LeaseStatus lease = client.getBlobLease(imageCheckerContext.getGroupName(), imageCheckerContext.getStorageName(),
                    imageCheckerContext.getContainerName(), imageCheckerContext.getSourceBlob());
            if (LeaseStatus.UNLOCKED.equals(lease)) {
                return true;
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
