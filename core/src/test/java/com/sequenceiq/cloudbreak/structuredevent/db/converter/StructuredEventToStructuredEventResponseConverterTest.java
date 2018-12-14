package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.audit.StructuredEventResponse;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class StructuredEventToStructuredEventResponseConverterTest {

    private StructuredEventToStructuredEventResponseConverter testedClass = new StructuredEventToStructuredEventResponseConverter();

    @Test
    public void convertRestCallEvent() {
        StructuredRestCallEvent source = new StructuredRestCallEvent();
        source.setType(StructuredRestCallEvent.class.getSimpleName());

        OperationDetails details = new OperationDetails();
        source.setOperation(details);

        RestCallDetails restOperations = new RestCallDetails();
        RestResponseDetails restResponseDetails = new RestResponseDetails();
        restResponseDetails.setStatusCode(200);
        restOperations.setRestResponse(restResponseDetails);
        source.setRestCall(restOperations);

        StructuredEventResponse response = testedClass.convert(source);
        assertTrue(JsonUtil.isValid(response.getRawRestEvent()));
    }

    @Test
    public void convertFlowEvent() {
        StructuredFlowEvent source = new StructuredFlowEvent();
        source.setType(StructuredFlowEvent.class.getSimpleName());

        OperationDetails details = new OperationDetails();
        source.setOperation(details);

        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setDuration(3445L);
        source.setFlow(flowDetails);

        StructuredEventResponse response = testedClass.convert(source);
        assertTrue(JsonUtil.isValid(response.getRawFlowEvent()));
    }

    @Test
    public void convertNotificationEvent() {
        StructuredNotificationEvent source = new StructuredNotificationEvent();
        source.setType(StructuredNotificationEvent.class.getSimpleName());

        OperationDetails details = new OperationDetails();
        source.setOperation(details);

        NotificationDetails notificationDetails = new NotificationDetails();
        source.setNotificationDetails(notificationDetails);

        StructuredEventResponse response = testedClass.convert(source);
        assertTrue(JsonUtil.isValid(response.getRawNotification()));
    }
}