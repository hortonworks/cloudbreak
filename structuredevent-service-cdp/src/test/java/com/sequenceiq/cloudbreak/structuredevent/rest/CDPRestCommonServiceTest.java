package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;

@ExtendWith(MockitoExtension.class)
class CDPRestCommonServiceTest {

    public static final String CLUSTER_NAME = "cluserName";

    public static final String CLUSTER_CRN = "clusterCrn";

    @InjectMocks
    private CDPRestCommonService underTest;

    private Map<String, CustomCrnOrNameProvider> customCrnOrNameProviders = new HashMap<>();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "customCrnOrNameProviders", customCrnOrNameProviders);
    }

    @Test
    void testWhenNameAndResourceCrnComeFromTheMapParameterButExistInRequestAndResponse() {
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
        Map<String, String> actual = underTest.collectCrnAndNameIfPresent(restCallDetails, null, restParams, CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    void testWhenNameAndResourceCrnComeFromTheRequestBodyButExistInResponse() {
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
        Map<String, String> actual = underTest.collectCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    void testWhenNameAndResourceCrnComeFromTheResponse() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        RestResponseDetails response = new RestResponseDetails();
        response.setBody(new Json(Map.of("name", "name3", "resourceCrn", "crn3")).getValue());
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "name3");
        expected.put(CLUSTER_CRN, "crn3");
        Map<String, String> actual = underTest.collectCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    void testWhenNamesAndCrnAreList() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setBody(new Json(Map.of("names", List.of("names1", "names2"), "crns", List.of("crns1", "crns2"))).getValue());
        RestResponseDetails response = new RestResponseDetails();
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, Object> expected = new HashMap<>();
        expected.put(CLUSTER_NAME, "names1,names2");
        expected.put(CLUSTER_CRN, "crns1,crns2");
        Map<String, String> actual = underTest.collectCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN);
        assertEquals(expected, actual);
    }

    @Test
    void testWhenNameAndResourceCrnNotExists() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        request.setMethod("POST");
        request.setRequestUri("uri");
        RestResponseDetails response = new RestResponseDetails();
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> underTest.collectCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), CLUSTER_NAME, CLUSTER_CRN));
        assertEquals(exception.getMessage(), "Cannot determine the resource crn or name, so we does not support for auditing for method: "
                + "POST, uri: uri, body: null");
    }

    @Test
    void testWhenNameAndResourceCrnComeFromTheResponsesValue() {
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
        Map<String, String> actual = underTest.collectCrnAndNameIfPresent(restCallDetails, null, Collections.emptyMap(), "names", "crns");
        assertEquals(expected, actual);
    }

    @Test
    void testWhenNameAndResourceCrnComeFromTheOperationDetails() {
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
        Map<String, String> actual = underTest.collectCrnAndNameIfPresent(restCallDetails, operationDetails, Collections.emptyMap(), "names", "crns");
        assertEquals(expected, actual);
    }

    @Test
    void testWhenNameAndResourceCrnComeFromTheNameOrCrnProvider() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails request = new RestRequestDetails();
        RestResponseDetails response = new RestResponseDetails();
        CDPOperationDetails operationDetails = new CDPOperationDetails();
        restCallDetails.setRestRequest(request);
        restCallDetails.setRestResponse(response);
        Map<String, String> restParams = new HashMap<>();
        restParams.put(RESOURCE_TYPE, "mock");

        CustomCrnOrNameProvider customCrnOrNameProvider = mock(CustomCrnOrNameProvider.class);
        customCrnOrNameProviders.put("mockCustomCrnOrNameProvider", customCrnOrNameProvider);
        when(customCrnOrNameProvider.provide(restCallDetails, operationDetails, restParams, "names", "crns"))
                .thenReturn(Map.of("crns", "pCrn", "names", "pName"));

        Map<String, Object> expected = new HashMap<>();
        expected.put("names", "pName");
        expected.put("crns", "pCrn");
        Map<String, String> actual = underTest.collectCrnAndNameIfPresent(restCallDetails, operationDetails, restParams, "names", "crns");
        assertEquals(expected, actual);
    }
}
