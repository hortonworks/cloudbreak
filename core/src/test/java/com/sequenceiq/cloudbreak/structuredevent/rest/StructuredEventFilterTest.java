package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.flow.ha.NodeConfig;

@ExtendWith(MockitoExtension.class)
class StructuredEventFilterTest {

    @InjectMocks
    private StructuredEventFilter underTest;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private StructuredEventClient structuredEventClient;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private NodeConfig nodeConfig;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "restUrlParsers", new ArrayList<>());
        ReflectionTestUtils.setField(underTest, "contentLogging", true);
    }

    @Test
    void filter() throws IOException {
        MultivaluedMap<String, String> headersMap = createRequestHeader();
        ContainerRequest requestContext = createRequestContext(headersMap);

        underTest.filter(requestContext);

        RestRequestDetails requestDetais = (RestRequestDetails) requestContext.getProperty("REQUEST_DETAIS");

        headersMap.forEach((key, value) -> assertEquals(value.get(0), requestDetais.getHeaders().get(key)));
    }

    private ContainerRequest createRequestContext(MultivaluedMap<String, String> headersMap) {
        MapPropertiesDelegate propertiesDelegate = new MapPropertiesDelegate();
        ContainerRequest requestContext =
                new ContainerRequest(URI.create("http://localhost"), URI.create("/test/endpoint"), "PUT", securityContext, propertiesDelegate);
        requestContext.headers(headersMap);
        return requestContext;
    }

    private MultivaluedMap<String, String> createRequestHeader() {
        MultivaluedMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put("x-forwarded-proto", List.of("http"));
        headersMap.put("x-forwarded-port", List.of("8080"));
        headersMap.put("x-forwarded-for", List.of("192.168.99.100"));
        headersMap.put("x-real-ip", List.of("192.168.99.100"));
        headersMap.put("x-forwarded-server", List.of("192.168.99.1"));
        headersMap.put("x-forwarded-host", List.of("192.168.99.1"));
        headersMap.put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON));
        return headersMap;
    }

    @Test
    void filterWithResponse() throws IOException {
        MultivaluedMap<String, String> headersMap = createRequestHeader();
        ContainerRequest requestContext = createRequestContext(headersMap);
        underTest.filter(requestContext);

        requestContext.setProperty("structuredevent.loggingEnabled", Boolean.TRUE);

        ArgumentCaptor<StructuredRestCallEvent> structuredEventCaptor = ArgumentCaptor.forClass(StructuredRestCallEvent.class);
        doNothing().when(structuredEventClient).sendStructuredEvent(structuredEventCaptor.capture());

        ContainerResponseContext responseContext = new ContainerResponse(requestContext, Response.accepted().build());
        underTest.filter(requestContext, responseContext);

        StructuredRestCallEvent captorValue = structuredEventCaptor.getValue();
        headersMap.forEach((key, value) -> assertEquals(value.get(0), captorValue.getRestCall().getRestRequest().getHeaders().get(key)));
    }

    @Test
    public void testResourceIdParsingWhenValidJsonIsReturned() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params, "{\"id\": \"12345\"}");
        assertEquals(params.get(RESOURCE_ID), "12345", "Should find resourceId in valid JSON response");
    }

    @Test
    public void testResourceCrnParsingWhenValidJsonIsReturned() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params, "{\"crn\": \"crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808\"}");
        assertEquals(params.get(RESOURCE_CRN), "crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808",
                "Should find resourceCrn in valid JSON response");
    }

    @Test
    public void testResourceCrnAndIdParsingWhenValidJsonIsReturned() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params,
                "{\"crn\": \"crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808\", \"id\": \"12345\"}");
        assertEquals(params.get(RESOURCE_CRN), "crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808",
                "Should find resourceCrn in valid JSON response");
        assertEquals(params.get(RESOURCE_ID), "12345", "Should find resourceId in valid JSON response");
    }

    @Test
    public void testResourceIdParsingWhenNonJson() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params, "\"id\":12345 Something other written here");
        assertEquals(params.get(RESOURCE_ID), "12345", "Should find resourceId in response");
    }

    @Test
    public void testResourceCrnParsingWhenNonJson() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params,
                "\"crn\":\"crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808\" Something other written here");
        assertEquals(params.get(RESOURCE_CRN), "crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808",
                "Should find resourceCrn in response");
    }

    @Test
    public void testResourceCrnAndIdParsingWhenNonJson() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params,
                "\"crn\":\"crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808\" Something other written here "
                        + "\"id\":12345 Something other written here");
        assertEquals(params.get(RESOURCE_CRN), "crn:cdp:datahub:us-west-1:cloudera:cluster:b20f9cb7-05fa-48ce-b249-02f5ab755808",
                "Should find resourceCrn in response");
        assertEquals(params.get(RESOURCE_ID), "12345", "Should find resourceId in response");
    }

    @Test
    public void testResourceIdParsingWhenJsonButNoId() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params, "{\"message\": \"Error happened and responding with JSON\"}");
        assertTrue(params.isEmpty(), "No ResourceId is present in responseBody");
    }

    @Test
    public void testResourceIdParsingWhenPlainTextResponse() {
        Map<String, String> params = Collections.emptyMap();
        underTest.extendRestParamsFromResponse(params,
                "The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character");
        assertTrue(params.isEmpty(), "No ResourceId is present in responseBody");
    }
}