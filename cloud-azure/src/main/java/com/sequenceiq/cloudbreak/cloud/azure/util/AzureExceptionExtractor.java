package com.sequenceiq.cloudbreak.cloud.azure.util;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class AzureExceptionExtractor {

    public static final String AZURE_EMBEDDED_ERROR_DESCRIPTION_ATTRIBUTE = "error_description";

    public String extractErrorMessage(Throwable t) {
        Throwable rootCause = ExceptionUtils.getRootCause(t);
        return getIfNotNull(rootCause, this::getTransformedErrorMessage);
    }

    private String getTransformedErrorMessage(Throwable rc) {
        String errorDescription = rc.getMessage();
        if (errorDescription != null) {
            JsonNode json;
            try {
                json = JsonUtil.readTree(errorDescription);
                errorDescription = getIfNotNullOtherwise(json.get(AZURE_EMBEDDED_ERROR_DESCRIPTION_ATTRIBUTE), JsonNode::asText, errorDescription);
            } catch (IOException ignore) {
                // not a JSON, no problem
            }
        }
        return errorDescription;
    }
}
