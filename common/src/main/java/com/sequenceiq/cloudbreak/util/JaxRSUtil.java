package com.sequenceiq.cloudbreak.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;

public class JaxRSUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRSUtil.class);

    private JaxRSUtil() {
    }

    public static <T> T response(Response response, Class<T> clazz) {
        if (Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            if (!response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                throw handleUnexpectedError(response);
            }
        }
        try {
            response.bufferEntity();
            return response.readEntity(clazz);
        } catch (Exception e) {
            LOGGER.warn("Couldn't parse response: [{}]", response, e);
            throw handleUnexpectedError(response);
        } finally {
            LOGGER.debug("Original salt response: {}", AnonymizerUtil.anonymize(response.readEntity(String.class)));
            response.close();
        }
    }

    private static WebApplicationException handleUnexpectedError(Response response) {
        String textResponse = response.readEntity(String.class);
        LOGGER.debug("Received error: {}", textResponse);
        String errormsg = "Status: " + response.getStatusInfo().getStatusCode() + ' ' + response.getStatusInfo().getReasonPhrase()
                + " Response: " + textResponse;
        return new WebApplicationException(errormsg);
    }
}