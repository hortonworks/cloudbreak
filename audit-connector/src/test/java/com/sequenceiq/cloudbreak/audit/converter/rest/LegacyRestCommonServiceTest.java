package com.sequenceiq.cloudbreak.audit.converter.rest;

import static com.sequenceiq.cloudbreak.audit.converter.EventDataExtractor.CLUSTER_CRN;
import static com.sequenceiq.cloudbreak.audit.converter.EventDataExtractor.CLUSTER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyRestCommonService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;

@ExtendWith(MockitoExtension.class)
public class LegacyRestCommonServiceTest {

    @InjectMocks
    private LegacyRestCommonService legacyRestCommonService;

    @Test
    public void testWhenNameAndResourceCrnComeFromTheOperationButExistInRequestAndResponse() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        operation.setResourceCrn("crn1");
        operation.setResourceName("name1");
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setBody(new Json(Map.of("name", "name2", "resourceCrn", "crn2")).getValue());
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("name", "name3", "resourceCrn", "crn3")).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        event.setRestCall(restCallDetails);
        event.setOperation(operation);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "name1");
        expected.put(CLUSTER_CRN, "crn1");
        Map<String, Object> actual = legacyRestCommonService.addClusterCrnAndNameIfPresent(event);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNameAndResourceCrnComeFromTheRequestBodyButExistInResponse() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setBody(new Json(Map.of("name", "name2", "resourceCrn", "crn2")).getValue());
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("name", "name3", "resourceCrn", "crn3")).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        event.setRestCall(restCallDetails);
        event.setOperation(operation);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "name2");
        expected.put(CLUSTER_CRN, "crn2");
        Map<String, Object> actual = legacyRestCommonService.addClusterCrnAndNameIfPresent(event);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNameAndResourceCrnComeFromTheResponse() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("name", "name3", "resourceCrn", "crn3")).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        event.setRestCall(restCallDetails);
        event.setOperation(operation);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "name3");
        expected.put(CLUSTER_CRN, "crn3");
        Map<String, Object> actual = legacyRestCommonService.addClusterCrnAndNameIfPresent(event);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNamesAndCrnAreListAndMethodDelete() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setMethod("DELETE");
        request.setBody(new Json(Map.of("names", List.of("names1", "names2"), "crns", List.of("crns1", "crns2"))).getValue());
        RestResponseDetails response = new RestResponseDetails();
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        event.setRestCall(restCallDetails);
        event.setOperation(operation);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "names1,names2");
        expected.put(CLUSTER_CRN, "crns1,crns2");
        Map<String, Object> actual = legacyRestCommonService.addClusterCrnAndNameIfPresent(event);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNameAndResourceCrnNotExists() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setMethod("POST");
        request.setRequestUri("uri");
        RestResponseDetails response = new RestResponseDetails();
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        event.setRestCall(restCallDetails);
        event.setOperation(operation);
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> legacyRestCommonService.addClusterCrnAndNameIfPresent(event));
        assertEquals(exception.getMessage(), "Cannot determine the resource crn or name, so we does not support for auditing for method: "
                + "POST, uri: uri, body: null");
    }
}
