package com.sequenceiq.cloudbreak.structuredevent.converter;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static org.junit.Assert.assertEquals;

public class StructuredNotificationEventToCloudbreakEventJsonConverterTest extends AbstractEntityConverterTest<StructuredNotificationEvent> {
    private StructuredNotificationEventToCloudbreakEventJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new StructuredNotificationEventToCloudbreakEventJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CloudbreakEventsJson result = underTest.convert(getSource());
        // THEN
        assertEquals("message", result.getEventMessage());
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone"));
    }

    @Override
    public StructuredNotificationEvent createSource() {
        OperationDetails operation = new OperationDetails(Calendar.getInstance().getTimeInMillis(), "NOTIFICATION", "STACK", 1L,
                "account", "owner", "userName", "cbId", "cbVersion");
        NotificationDetails notification = new NotificationDetails();
        notification.setInstanceGroup("master");
        notification.setRegion("us");
        notification.setStackName("usagestack");
        notification.setStackId(1L);
        notification.setNotification("message");
        notification.setNotificationType("eventType");
        notification.setCloud(GCP);
        notification.setBlueprintName("blueprintName");
        notification.setBlueprintId(1L);
        notification.setStackStatus(AVAILABLE.name());
        notification.setNodeCount(1);
        notification.setClusterStatus(AVAILABLE.name());
        notification.setClusterId(1L);
        notification.setClusterName("test");
        return new StructuredNotificationEvent(operation, notification);
    }
}
