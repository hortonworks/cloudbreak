package com.sequenceiq.cloudbreak.common.exception;

import java.io.IOException;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class ClientErrorExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientErrorExceptionHandler.class);

    private ClientErrorExceptionHandler() { }

    public static String getErrorMessage(ClientErrorException cee) {
        try (Response response = cee.getResponse()) {
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
    }
}
