package com.sequenceiq.it.cloudbreak.util;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class ResponseUtil {
    private ResponseUtil() {

    }

    public static String getErrorMessage(Exception ex) {
        return getFieldOfErrorResponse(ex, "message");
    }

    public static String getErrorPayload(Exception ex) {
        return getFieldOfErrorResponse(ex, "payload");
    }

    private static String getFieldOfErrorResponse(Exception ex, String field) {
        if (ex instanceof WebApplicationException) {
            try {
                String responseJson = ((WebApplicationException) ex).getResponse().readEntity(String.class);
                if (JsonUtil.isValid(responseJson)) {
                    JsonNode jsonNode = JsonUtil.readTree(responseJson);
                    if (jsonNode.has(field)) {
                        if (jsonNode.get(field).isTextual()) {
                            return jsonNode.get(field).asText();
                        }
                        return jsonNode.get(field).toString();
                    }
                }
                return responseJson;
            } catch (IOException ignore) {
            }
        }
        return ex.getMessage();
    }
}
