package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderSetupAdapter;

@Component
public class ImageStatusCheckerTask extends StackBasedStatusCheckerTask<ImageCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageStatusCheckerTask.class);

    @Inject
    private ServiceProviderSetupAdapter provisioning;

    @Inject
    private NotificationSender notificationSender;

    @Override
    public boolean checkStatus(ImageCheckerContext t) {
        try {
            ImageStatusResult imageStatusResult = provisioning.checkImage(t.getStack());
            if (imageStatusResult.getImageStatus().equals(ImageStatus.CREATE_FAILED)) {
                notificationSender.send(getImageCopyNotification(imageStatusResult, t.getStack()));
                throw new CloudbreakServiceException("Image copy operation finished with failed status.");
            } else if (imageStatusResult.getImageStatus().equals(ImageStatus.CREATE_FINISHED)) {
                notificationSender.send(getImageCopyNotification(imageStatusResult, t.getStack()));
                return true;
            } else {
                notificationSender.send(getImageCopyNotification(imageStatusResult, t.getStack()));
                return false;
            }
        } catch (Exception e) {
            throw new CloudbreakServiceException(e);
        }
    }

    private Notification<CloudbreakEventsJson> getImageCopyNotification(ImageStatusResult result, Stack stack) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventType("IMAGE_COPY_STATE");
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(String.valueOf(result.getStatusProgressValue()));
        notification.setUserId(stack.getCreator().getUserId());
        notification.setWorkspaceId(stack.getWorkspace().getId());
        notification.setCloud(stack.cloudPlatform());
        notification.setRegion(stack.getRegion());
        notification.setStackId(stack.getId());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        return new Notification<>(notification);
    }

    @Override
    public void handleTimeout(ImageCheckerContext t) {
        throw new CloudbreakServiceException("Operation timed out. Image copy operation failed.");
    }

    @Override
    public String successMessage(ImageCheckerContext t) {
        return "Image copy operation finished with success state.";
    }

}
