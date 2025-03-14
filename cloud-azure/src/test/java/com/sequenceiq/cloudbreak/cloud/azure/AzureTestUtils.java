package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.azure.core.management.exception.ManagementError;
import com.azure.resourcemanager.compute.models.ApiError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AzureTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTestUtils.class);

    private AzureTestUtils() {
    }

    public static ApiError apiError(String code, String message) {
        ApiError apiError = new ApiError();
        setField(apiError, "code", code);
        setField(apiError, "message", message);
        return apiError;
    }

    public static ManagementError managementError(String code, String message) {
        ManagementError managementError = new ManagementError();
        setField(managementError, "code", code);
        setField(managementError, "message", message);
        return managementError;
    }

    public static void setDetails(ManagementError apiError, List<ManagementError> details) {
        setField(apiError, "details", details);
    }

    public static void setField(Object object, String fieldName, Object fieldValue) {
        Field field = ReflectionUtils.findField(object.getClass(), fieldName);
        field.setAccessible(true);
        ReflectionUtils.setField(field, object, fieldValue);
    }

    public static void validateJson(String templateString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readTree(templateString);
        } catch (JsonProcessingException jpe) {
            int contextLines = 2;
            int lineNumberOfIssue = jpe.getLocation().getLineNr();
            List<String> lines = templateString.lines().collect(Collectors.toList());
            int startingIndex = Math.max(lineNumberOfIssue - (contextLines + 1), 0);
            int endingIndex = Math.min(lineNumberOfIssue + contextLines, lines.size() - 1);

            List<String> context = lines.subList(startingIndex, endingIndex);

            String message = String.join("\n", context);
            LOGGER.warn("Error reading String as JSON at line {}:\n{}", lineNumberOfIssue, message);
            fail("Generated ARM template is not valid JSON.\n" + jpe.getMessage());
        }
    }
}
