package com.sequenceiq.cloudbreak.cm.util;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.client.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class ClouderaManagerApiExceptionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerApiExceptionUtil.class);

    private ClouderaManagerApiExceptionUtil() {
    }

    /**
     * Extracts a human-readable message from a Cloudera Manager {@link ApiException}. CM returns the meaningful detail in the response body (typically a JSON
     * object with a {@code message} field), while {@link ApiException#getMessage()} only carries the HTTP reason phrase (e.g. "Bad Request"). This prefers the
     * response body's {@code message} field, then the raw response body, and finally falls back to the HTTP message.
     */
    public static String extractMessage(ApiException apiException) {
        if (StringUtils.isEmpty(apiException.getResponseBody())) {
            return apiException.getMessage();
        }
        try {
            JsonNode tree = JsonUtil.readTree(apiException.getResponseBody());
            JsonNode message = tree.get("message");
            if (message != null && message.isTextual()) {
                String text = message.asText();
                if (StringUtils.isNotEmpty(text)) {
                    return text;
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to parse Cloudera Manager API response body as JSON", e);
        }
        return apiException.getResponseBody();
    }
}
