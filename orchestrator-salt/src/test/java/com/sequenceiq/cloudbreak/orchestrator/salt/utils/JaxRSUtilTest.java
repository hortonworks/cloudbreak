package com.sequenceiq.cloudbreak.orchestrator.salt.utils;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyWebApplicationException;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.util.JaxRSUtil;

@ExtendWith(MockitoExtension.class)
class JaxRSUtilTest {

    @Mock
    private Response response;

    @Mock
    private StatusType statusType;

    @Test
    void testNoMediaType() {
        when(response.getMediaType()).thenReturn(null);
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(statusType.getStatusCode()).thenReturn(200);
        when(statusType.getReasonPhrase()).thenReturn("OK");
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
        assertEquals("Status: 200 OK Media Type: null Response: null", webApplicationException.getMessage());
    }

    @Test
    void testIncompatibleMediaType() {
        when(response.getMediaType()).thenReturn(MediaType.TEXT_HTML_TYPE);
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(statusType.getStatusCode()).thenReturn(200);
        when(statusType.getReasonPhrase()).thenReturn("OK");
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
        assertEquals("Status: 200 OK Media Type: text/html Response: null", webApplicationException.getMessage());
    }

    @Test
    void testReadEntity() {
        when(response.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(response.bufferEntity()).thenReturn(true);
        GenericResponses value = new GenericResponses();
        when(response.readEntity(GenericResponses.class)).thenReturn(value);
        assertEquals(value, JaxRSUtil.response(response, GenericResponses.class));
    }

    @Test
    void testUnsuccessfulResponse() {
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Family.SERVER_ERROR);
        when(statusType.getStatusCode()).thenReturn(500);
        when(statusType.getReasonPhrase()).thenReturn("SERVER ERROR");
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
        assertEquals("Status: 500 SERVER ERROR Response: null", webApplicationException.getMessage());
    }

    @Test
    void testClusterProxyError() {
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Family.SERVER_ERROR);
        when(statusType.getStatusCode()).thenReturn(599);
        when(statusType.getReasonPhrase()).thenReturn("Network connect timeout");
        when(response.readEntity(any(Class.class))).thenReturn("{\"status\":599,\"code\":\"cluster-proxy.proxy.timeout\",\"message\":" +
                "\"Connect timeout of Some(10 seconds) expired\",\"retryable\":true}");
        WebApplicationException cpException = assertThrows(ClusterProxyWebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
        assertTrue(cpException instanceof ClusterProxyWebApplicationException);

        when(response.readEntity(any(Class.class))).thenReturn("{\"status\":599,\"code\":\"cluster-proxy.proxy.timeout\",\"message\":" +
                "\"Connect timeout of Some(10 seconds) expired\",\"retryable\":false}");
        cpException = assertThrows(WebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
        assertFalse(cpException instanceof ClusterProxyWebApplicationException);

        when(response.readEntity(any(Class.class))).thenReturn("{\"random\":true}");
        cpException = assertThrows(WebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
        assertFalse(cpException instanceof ClusterProxyWebApplicationException);

        when(response.readEntity(any(Class.class))).thenReturn("not a json");
        cpException = assertThrows(WebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
        assertFalse(cpException instanceof ClusterProxyWebApplicationException);
    }
}
