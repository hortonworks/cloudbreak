package com.sequenceiq.cloudbreak.structuredevent.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;

@ExtendWith(MockitoExtension.class)
public class CDPRestCommonServiceTest {

    public static final String CLUSTER_NAME = "cluserName";

    public static final String CLUSTER_CRN = "clusterCrn";

    @InjectMocks
    private CDPRestCommonService underTest;

    @Test
    public void testWhenNameAndResourceCrnComeFromTheMapParameterButExistInRequestAndResponse() {
        Map<String, String> restParams = Map.of(CLUSTER_NAME, "name1", CLUSTER_CRN, "crn1");
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setBody(new Json(Map.of("name", "name2", "resourceCrn", "crn2")).getValue());
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("name", "name3", "resourceCrn", "crn3")).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, String> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "name1");
        expected.put(CLUSTER_CRN, "crn1");
        Map<String, String> actual = underTest.addClusterCrnAndNameIfPresent(restCallDetails, null, restParams, CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNameAndResourceCrnComeFromTheRequestBodyButExistInResponse() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setBody(new Json(Map.of("name", "name2", "resourceCrn", "crn2")).getValue());
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("name", "name3", "resourceCrn", "crn3")).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "name2");
        expected.put(CLUSTER_CRN, "crn2");
        Map<String, String> actual = underTest.addClusterCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNameAndResourceCrnComeFromTheResponse() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("name", "name3", "resourceCrn", "crn3")).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "name3");
        expected.put(CLUSTER_CRN, "crn3");
        Map<String, String> actual = underTest.addClusterCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNamesAndCrnAreList() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setBody(new Json(Map.of("names", List.of("names1", "names2"), "crns", List.of("crns1", "crns2"))).getValue());
        RestResponseDetails response = new RestResponseDetails();
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "names1,names2");
        expected.put(CLUSTER_CRN, "crns1,crns2");
        Map<String, String> actual = underTest.addClusterCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNameAndResourceCrnNotExists() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setMethod("POST");
        request.setRequestUri("uri");
        RestResponseDetails response = new RestResponseDetails();
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> underTest.addClusterCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN));
        assertEquals(exception.getMessage(), "Cannot determine the resource crn or name, so we does not support for auditing for method: "
                + "POST, uri: uri, body: null");
    }

    @Test
    public void testWhenNameAndResourceCrnComeFromTheResponsesValue() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("responses", List.of(
                Map.of("name", "name1", "crn", "crn1"),
                Map.of("name", "name2", "crn", "crn2")
        ))).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, Object> expected = new HashMap<>();
        expected.put("names", "name1,name2");
        expected.put("crns", "crn1,crn2");
        Map<String, String> actual = underTest.addClusterCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), "names", "crns");
        assertEquals(expected, actual);
    }

    @Test
    public void testWhenNameAndResourceCrnComeFromTheOperationDetails() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("responses", List.of(
                Map.of("name", "name1", "crn", "crn1"),
                Map.of("name", "name2", "crn", "crn2")
        ))).getValue());
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setResourceCrn("opCrn");
        operationDetails.setResourceName("opName");
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, Object> expected = new HashMap<>();
        expected.put("names", "opName");
        expected.put("crns", "opCrn");
        Map<String, String> actual = underTest.addClusterCrnAndNameIfPresent(restCallDetails, operationDetails, Collections.emptyMap(), "names", "crns");
        assertEquals(expected, actual);
    }
}
