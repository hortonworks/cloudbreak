package com.sequenceiq.cloudbreak.common.exception;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class WebApplicationExceptionMessageExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionMessageExtractor.class);

    public String getErrorMessage(Exception exception) {
        String errorMessage = exception.getMessage();
        if (exception instanceof WebApplicationException) {
            errorMessage = getErrorMessage((WebApplicationException) exception);
        }
        return errorMessage;
    }

    public String getErrorMessage(WebApplicationException exception) {
        try (Response response = exception.getResponse()) {
            if (response.hasEntity()) {
                String errorResponse = response.readEntity(String.class);
                LOGGER.info("Client error response is " + errorResponse);
                try {
                    JsonNode jsonNode = JsonUtil.readTree(errorResponse);
                    if (jsonNode.has("validationErrors")) {
                        return "Validation error: " + jsonNode.get("validationErrors");
                    }
                    if (jsonNode.has("message")) {
                        return "Error message: " + jsonNode.get("message");
                    }
                    return errorResponse;
                } catch (IOException e) {
                    LOGGER.error("Can not parse response to json node", e);
                    return errorResponse;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot extract error response", e);
        }
        return exception.getMessage();
    }
}
