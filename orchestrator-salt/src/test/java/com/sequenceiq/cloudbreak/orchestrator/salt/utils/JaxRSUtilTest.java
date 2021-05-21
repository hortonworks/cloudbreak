package com.sequenceiq.cloudbreak.orchestrator.salt.utils;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.util.JaxRSUtil;

@RunWith(MockitoJUnitRunner.class)
public class JaxRSUtilTest {

    @Mock
    private Response response;

    @Mock
    private StatusType statusType;

    @Test
    public void testNoMediaType() {
        when(response.getMediaType()).thenReturn(null);
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Family.SERVER_ERROR);
        assertThrows(WebApplicationException.class, () ->
                JaxRSUtil.response(response, GenericResponses.class));
    }

    @Test
    public void testReadEntity() {
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(response.bufferEntity()).thenReturn(true);
        GenericResponses value = new GenericResponses();
        when(response.readEntity(GenericResponses.class)).thenReturn(value);
        assertEquals(value, JaxRSUtil.response(response, GenericResponses.class));
    }
}
