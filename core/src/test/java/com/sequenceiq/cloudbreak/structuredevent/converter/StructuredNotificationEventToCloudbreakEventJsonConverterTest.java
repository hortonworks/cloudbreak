package com.sequenceiq.cloudbreak.structuredevent.converter;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@RunWith(MockitoJUnitRunner.class)
public class StructuredNotificationEventToCloudbreakEventJsonConverterTest extends AbstractEntityConverterTest<StructuredNotificationEvent> {

    private static final long ORG_ID = 1L;

    private static final String USER_ID = "horton@hortonworks.com";

    private static final String USER_NAME = "Alma Ur";

    @InjectMocks
    private StructuredNotificationEventToCloudbreakEventJsonConverter underTest;

    @Mock
    private StackApiViewService stackApiViewService;

    @Test
    public void testConvert() {
        CloudbreakEventsJson result = underTest.convert(getSource());
        assertEquals("message", result.getEventMessage());
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone", "owner", "account", "stackView"));
    }

    @Override
    public StructuredNotificationEvent createSource() {
        OperationDetails operation = new OperationDetails(Calendar.getInstance().getTimeInMillis(), NOTIFICATION,
                "stacks", 1L, "usagestack", USER_ID, USER_NAME, "cbId",
                "cbVersion", ORG_ID, "account", "owner", "userName");
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
