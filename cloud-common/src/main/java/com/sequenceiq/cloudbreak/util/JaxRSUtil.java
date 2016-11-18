package com.sequenceiq.cloudbreak.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxRSUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRSUtil.class);

    private JaxRSUtil() {
    }

    public static <T> T response(Response response, Class<T> clazz) {
        if (Response.Status.Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            if (!response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                String errormsg = "Status: " + response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase();
                String textResponse = response.readEntity(String.class);
                LOGGER.error("Received error: {}", textResponse);
                throw new WebApplicationException(errormsg);
            }
        }
        return response.readEntity(clazz);
    }
}