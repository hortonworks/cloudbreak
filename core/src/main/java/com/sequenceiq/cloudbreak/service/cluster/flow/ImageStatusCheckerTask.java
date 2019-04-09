package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakV4Event;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.ProgressUpdateV4Event;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.notification.NotificationAssemblingService;
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
        ImageStatusResult imageStatusResult = getImageStatusResult(t);
        NotificationEventType eventType = getNotificationEventType(imageStatusResult);

        notificationSender.send(getImageCopyNotification(imageStatusResult, t.getStack(), eventType));
        //TODO: remove notifiaction backward compatible
        notificationSender.send(getImageCopyNotification(imageStatusResult, t.getStack()));

        if (imageStatusResult.getImageStatus().equals(ImageStatus.CREATE_FAILED)) {
            throw new CloudbreakServiceException("Image copy operation finished with failed status.");
        }
        return eventType == NotificationEventType.IMAGE_COPY_SUCCESS;
    }

    private ImageStatusResult getImageStatusResult(ImageCheckerContext t) {
        try {
            return provisioning.checkImage(t.getStack());
        } catch (Exception e) {
            throw new CloudbreakServiceException(e);
        }
    }

    private Notification<CloudbreakEventV4Response> getImageCopyNotification(ImageStatusResult result, Stack stack) {
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
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
        notification.setTenantName(stack.getCreator().getTenant().getName());
        return new Notification<>(notification);
    }

    private NotificationEventType getNotificationEventType(ImageStatusResult imageStatusResult) {
        NotificationEventType eventType = NotificationEventType.IMAGE_COPY_IN_PROGRESS;
        if (imageStatusResult.getImageStatus().equals(ImageStatus.CREATE_FAILED)) {
            eventType = NotificationEventType.IMAGE_COPY_FAILED;
        } else if (imageStatusResult.getImageStatus().equals(ImageStatus.CREATE_FINISHED)) {
            eventType = NotificationEventType.IMAGE_COPY_SUCCESS;
        }
        return eventType;
    }

    private Notification<CloudbreakV4Event> getImageCopyNotification(ImageStatusResult result, Stack stack, NotificationEventType eventType) {
        ProgressUpdateV4Event payload = new ProgressUpdateV4Event().withProgress(result.getStatusProgressValue().longValue());
        CloudbreakV4Event response = NotificationAssemblingService.cloudbreakEvent(payload, eventType, WorkspaceResource.IMAGE_CATALOG);
        response.setWorkspaceId(stack.getWorkspace().getId());
        response.setUser(stack.getCreator().getUserId());
        return new Notification<>(response);
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
