package com.sequenceiq.cloudbreak.cloud.azure;

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.util.ReflectionUtils;

import com.azure.core.management.exception.ManagementError;
import com.azure.resourcemanager.compute.models.ApiError;

public class AzureTestUtils {

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
}
