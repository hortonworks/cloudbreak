package com.sequenceiq.cloudbreak.cloud.azure.util;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class AzureExceptionExtractor {
    public static final String AZURE_EMBEDDED_ERROR_DESCRIPTION_ATTRIBUTE = "error_description";

    public static final String AZURE_EMBEDDED_ERROR_ATTRIBUTE = "error";

    public static final String AZURE_EMBEDDED_ERROR_URI_ATTRIBUTE = "error_uri";

    public String extractErrorMessage(Throwable t) {
        Throwable rootCause = ExceptionUtils.getRootCause(t);
        return getIfNotNull(rootCause, this::getTransformedErrorMessage);
    }

    private String getTransformedErrorMessage(Throwable t) {
        if (t.getMessage() != null) {
            try {
                JsonNode json = JsonUtil.readTree(t.getMessage());
                String result = Stream.of(AZURE_EMBEDDED_ERROR_DESCRIPTION_ATTRIBUTE, AZURE_EMBEDDED_ERROR_ATTRIBUTE, AZURE_EMBEDDED_ERROR_URI_ATTRIBUTE)
                        .map(key -> getIfNotNullOtherwise(json.get(key), node -> key + ": " + node.asText(), ""))
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining(", "));
                return StringUtils.isBlank(result) ? t.getMessage() : result;
            } catch (IOException ignore) {
                // not a JSON, no problem
                return t.getMessage();
            }
        } else {
            return null;
        }
    }
}
