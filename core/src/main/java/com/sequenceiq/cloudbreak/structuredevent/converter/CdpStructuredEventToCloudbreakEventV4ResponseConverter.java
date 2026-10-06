package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;

/**
 * Maps a CDP structured event (as returned by redbeams' audit endpoint) into the {@link CloudbreakEventV4Response} shape the
 * Management Console consumes, so redbeams-originated events can be merged into a stack's event feed alongside core's legacy events.
 */
@Component
public class CdpStructuredEventToCloudbreakEventV4ResponseConverter {

    public CloudbreakEventV4Response convert(CDPStructuredEvent source) {
        CloudbreakEventV4Response cloudbreakEvent = new CloudbreakEventV4Response();
        CDPOperationDetails operation = source.getOperation();
        if (operation != null) {
            cloudbreakEvent.setEventTimestamp(operation.getTimestamp() == null ? 0L : operation.getTimestamp());
            String eventType = operation.getResourceEvent() != null ? operation.getResourceEvent()
                    : (operation.getEventType() == null ? null : operation.getEventType().name());
            cloudbreakEvent.setEventType(eventType);
            cloudbreakEvent.setNotificationType(eventType);
            cloudbreakEvent.setStackCrn(operation.getResourceCrn());
            cloudbreakEvent.setStackName(operation.getResourceName());
            cloudbreakEvent.setUserId(operation.getUserCrn());
        }
        cloudbreakEvent.setEventMessage(resolveMessage(source));
        return cloudbreakEvent;
    }

    private String resolveMessage(CDPStructuredEvent source) {
        if (StringUtils.isNotBlank(source.getStatusReason())) {
            return source.getStatusReason();
        }
        if (source instanceof CDPStructuredNotificationEvent notificationEvent
                && notificationEvent.getNotificationDetails() != null
                && notificationEvent.getNotificationDetails().getResourceEvent() != null) {
            return notificationEvent.getNotificationDetails().getResourceEvent().name();
        }
        return null;
    }
}
