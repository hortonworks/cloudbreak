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
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@RunWith(Parameterized.class)
public class StructuredWebSocketNotificationEventToCloudbreakEventJsonConverterTest extends AbstractEntityConverterTest<StructuredNotificationEvent> {

    private static final String MESSAGE = "someMessage";

    private static final String TYPE = "eventType";

    private StructuredNotificationEvent source;

    private List<String> skippedFields;

    private StructuredNotificationEventToCloudbreakEventV4ResponseConverter underTest;

    public StructuredWebSocketNotificationEventToCloudbreakEventJsonConverterTest(StructuredNotificationEvent source, List<String> additionalElementsToSkip) {
        skippedFields = Lists.newArrayList("availabilityZone");
        skippedFields.addAll(additionalElementsToSkip);
        this.source = source;
    }

    @Parameters(name = "Current StructuredNotificationEvent {0}, and the following fields should be skipped on null check: {1}")
    public static Object[][] data() {
        OperationDetails operation = new OperationDetails(Calendar.getInstance().getTimeInMillis(), NOTIFICATION, CloudbreakEventService.DATAHUB_RESOURCE_TYPE,
                1L, "usagestack", "cbId", "cbVersion", 1L, "horton@hortonworks.com", "Alma Ur", "tenant", "crn", "userCrn", "environemntCrn", "resourceEvent");
        return new Object[][]{
                {new StructuredNotificationEvent(operation, TestUtil.notificationDetails(MESSAGE, TYPE)), List.of("ldapDetails", "rdsDetails")},
                {new StructuredNotificationEvent(operation, TestUtil.ldapNotificationDetails(MESSAGE, TYPE)), getFieldNamesExcept(List.of("ldapDetails"))},
                {new StructuredNotificationEvent(operation, TestUtil.rdsNotificationDetails(MESSAGE, TYPE)), getFieldNamesExcept(List.of("rdsDetails"))}
        };
    }

    @Before
    public void setUp() {
        underTest = new StructuredNotificationEventToCloudbreakEventV4ResponseConverter();
    }

    @Test
    public void testConvert() {
        CloudbreakEventV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(MESSAGE, result.getEventMessage());
        assertEquals(TYPE, result.getEventType());
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
