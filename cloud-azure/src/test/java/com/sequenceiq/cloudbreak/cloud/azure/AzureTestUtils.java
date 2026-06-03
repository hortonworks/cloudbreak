package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.azure.core.management.exception.ManagementError;
import com.azure.json.JsonProviders;
import com.azure.resourcemanager.compute.models.ApiError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.json.Json;

public class AzureTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTestUtils.class);

    private AzureTestUtils() {
    }

    public static ApiError apiError(String code, String message) {
        try {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("code", code);
            errorMap.put("message", message);
            String json = new Json(Map.of("error", errorMap)).getValue();
            return ApiError.fromJson(JsonProviders.createReader(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ManagementError managementError(String code, String message) {
        return new ManagementError(code, message);
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
