package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;

class CdpStructuredEventToCloudbreakEventV4ResponseConverterTest {

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:acc:user:bob";

    private static final String DB_NAME = "my-external-db";

    private static final String MESSAGE = "External database my-external-db is low on storage: 7.3% free remaining.";

    private final CdpStructuredEventToCloudbreakEventV4ResponseConverter underTest = new CdpStructuredEventToCloudbreakEventV4ResponseConverter();

    @Test
    void convertMapsOperationFieldsAndStatusReason() {
        CDPStructuredNotificationEvent source = notificationEvent(1700000000000L, MESSAGE);

        CloudbreakEventV4Response result = underTest.convert(source);

        assertThat(result.getEventTimestamp()).isEqualTo(1700000000000L);
        assertThat(result.getEventType()).isEqualTo(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW.name());
        assertThat(result.getNotificationType()).isEqualTo(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW.name());
        assertThat(result.getStackCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(result.getStackName()).isEqualTo(DB_NAME);
        assertThat(result.getUserId()).isEqualTo(USER_CRN);
        assertThat(result.getEventMessage()).isEqualTo(MESSAGE);
    }

    @Test
    void convertFallsBackToResourceEventNameWhenStatusReasonBlank() {
        CDPStructuredNotificationEvent source = notificationEvent(1700000000000L, "  ");

        CloudbreakEventV4Response result = underTest.convert(source);

        assertThat(result.getEventMessage()).isEqualTo(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW.name());
    }

    @Test
    void convertUsesZeroTimestampWhenMissing() {
        CDPStructuredNotificationEvent source = notificationEvent(null, MESSAGE);

        CloudbreakEventV4Response result = underTest.convert(source);

        assertThat(result.getEventTimestamp()).isZero();
    }

    @Test
    void convertHandlesNullOperation() {
        CDPStructuredNotificationEvent source = new CDPStructuredNotificationEvent();

        CloudbreakEventV4Response result = underTest.convert(source);

        assertThat(result.getEventTimestamp()).isZero();
        assertThat(result.getEventType()).isNull();
        assertThat(result.getNotificationType()).isNull();
        assertThat(result.getStackCrn()).isNull();
        assertThat(result.getStackName()).isNull();
        assertThat(result.getUserId()).isNull();
        assertThat(result.getEventMessage()).isNull();
    }

    @Test
    void convertReturnsNullMessageWhenStatusReasonBlankAndNoNotificationDetails() {
        CDPOperationDetails operation = new CDPOperationDetails(100L, StructuredEventType.NOTIFICATION, "redbeams", 1L, DB_NAME,
                "node-1", "1.2.3", "acc", RESOURCE_CRN, USER_CRN, null, null);
        CDPStructuredNotificationEvent source = new CDPStructuredNotificationEvent(operation, null, "SENT", "  ");

        CloudbreakEventV4Response result = underTest.convert(source);

        assertThat(result.getEventMessage()).isNull();
    }

    @Test
    void convertFallsBackToEventTypeWhenResourceEventMissing() {
        CDPOperationDetails operation = new CDPOperationDetails(100L, StructuredEventType.NOTIFICATION, "redbeams", 1L, DB_NAME,
                "node-1", "1.2.3", "acc", RESOURCE_CRN, USER_CRN, null, null);
        CDPStructuredNotificationEvent source = new CDPStructuredNotificationEvent(operation, null, "SENT", MESSAGE);

        CloudbreakEventV4Response result = underTest.convert(source);

        assertThat(result.getEventType()).isEqualTo(StructuredEventType.NOTIFICATION.name());
    }

    private CDPStructuredNotificationEvent notificationEvent(Long timestamp, String statusReason) {
        ResourceEvent resourceEvent = ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW;
        CDPOperationDetails operation = new CDPOperationDetails(timestamp, StructuredEventType.NOTIFICATION, "redbeams", 1L, DB_NAME,
                "node-1", "1.2.3", "acc", RESOURCE_CRN, USER_CRN, null, resourceEvent.name());
        CDPStructuredNotificationDetails details = new CDPStructuredNotificationDetails(resourceEvent, RESOURCE_CRN, "redbeams", "{}");
        return new CDPStructuredNotificationEvent(operation, details, resourceEvent.name(), statusReason);
    }
}
