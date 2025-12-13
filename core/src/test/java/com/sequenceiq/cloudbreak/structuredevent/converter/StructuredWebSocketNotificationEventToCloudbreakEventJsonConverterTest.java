package com.sequenceiq.cloudbreak.structuredevent.converter;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

class StructuredWebSocketNotificationEventToCloudbreakEventJsonConverterTest extends AbstractEntityConverterTest<StructuredNotificationEvent> {

    private static final String MESSAGE = "someMessage";

    private static final String TYPE = "eventType";

    private StructuredNotificationEventToCloudbreakEventV4ResponseConverter underTest = new StructuredNotificationEventToCloudbreakEventV4ResponseConverter();

    public static Object[][] data() {
        OperationDetails operation = new OperationDetails(Calendar.getInstance().getTimeInMillis(), NOTIFICATION, CloudbreakEventService.DATAHUB_RESOURCE_TYPE,
                1L, "usagestack", "cbId", "cbVersion", 1L, "horton@hortonworks.com", "Alma Ur", "tenant", "crn", "userCrn", "environemntCrn", "resourceEvent");
        return new Object[][]{
                {new StructuredNotificationEvent(operation, TestUtil.notificationDetails(MESSAGE, TYPE)),
                        List.of("availabilityZone", "ldapDetails", "rdsDetails")},
                {new StructuredNotificationEvent(operation, TestUtil.ldapNotificationDetails(MESSAGE, TYPE)), getFieldNamesExcept(List.of("ldapDetails"))},
                {new StructuredNotificationEvent(operation, TestUtil.rdsNotificationDetails(MESSAGE, TYPE)), getFieldNamesExcept(List.of("rdsDetails"))}
        };
    }

    @MethodSource("data")
    @ParameterizedTest
    void testConvert(StructuredNotificationEvent source, List<String> skippedFields) {
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
