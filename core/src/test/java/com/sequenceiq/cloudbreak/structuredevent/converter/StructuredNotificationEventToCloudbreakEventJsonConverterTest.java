package com.sequenceiq.cloudbreak.structuredevent.converter;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@RunWith(Parameterized.class)
public class StructuredNotificationEventToCloudbreakEventJsonConverterTest extends AbstractEntityConverterTest<StructuredNotificationEvent> {

    private static final String MESSAGE = "someMessage";

    private StructuredNotificationEvent source;

    private List<String> skippedFields;

    private StructuredNotificationEventToCloudbreakEventJsonConverter underTest;

    public StructuredNotificationEventToCloudbreakEventJsonConverterTest(StructuredNotificationEvent source, List<String> additionalElementsToSkip) {
        skippedFields = Lists.newArrayList("availabilityZone");
        skippedFields.addAll(additionalElementsToSkip);
        this.source = source;
    }

    @Parameters(name = "Current StructuredNotificationEvent {0}, and the following fields should be skipped on null check: {1}")
    public static Object[][] data() {
        OperationDetails operation = new OperationDetails(Calendar.getInstance().getTimeInMillis(), NOTIFICATION, "stacks", 1L,
                "usagestack", "cbId", "cbVersion", 1L, "horton@hortonworks.com", "Alma Ur", "tenant");
        return new Object[][]{
                {new StructuredNotificationEvent(operation, TestUtil.notificationDetails(MESSAGE, NotificationEventType.CREATE_IN_PROGRESS)),
                        List.of("ldapDetails", "rdsDetails")},
                {new StructuredNotificationEvent(operation, TestUtil.ldapNotificationDetails(MESSAGE, NotificationEventType.CREATE_IN_PROGRESS)),
                        getFieldNamesExcept(List.of("ldapDetails"))},
                {new StructuredNotificationEvent(operation, TestUtil.rdsNotificationDetails(MESSAGE, NotificationEventType.CREATE_IN_PROGRESS)),
                        getFieldNamesExcept(List.of("rdsDetails"))}
        };
    }

    @Before
    public void setUp() {
        underTest = new StructuredNotificationEventToCloudbreakEventJsonConverter();
    }

    @Test
    public void testConvert() {
        CloudbreakEventV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(MESSAGE, result.getEventMessage());
        assertEquals(NotificationEventType.CREATE_IN_PROGRESS.name(), result.getEventType());
        assertAllFieldsNotNull(result, skippedFields);
    }

    @Override
    public StructuredNotificationEvent createSource() {
        return null;
    }

    private static List<String> getFieldNamesExcept(List<String> exclusions) {
        List<String> fieldsToReturn = new LinkedList<>();
        for (Field field : CloudbreakEventV4Response.class.getDeclaredFields()) {
            if (!exclusions.contains(field.getName())) {
                fieldsToReturn.add(field.getName());
            }
        }
        return fieldsToReturn;
    }

}
