package com.sequenceiq.environment.encryptionprofile.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

@Converter
public class ResourceStatusConverter implements AttributeConverter<ResourceStatus, String> {
    /**
     * This converter is used to convert the ResourceStatus enum to a String for database storage.
     * It defaults to ResourceStatus.USER_MANAGED if the input is null.
     */
    @Override
    public String convertToDatabaseColumn(ResourceStatus resourceStatus) {
        if (resourceStatus != null) {
            return resourceStatus.name();
        }
        return ResourceStatus.USER_MANAGED.name();
    }

    @Override
    public ResourceStatus convertToEntityAttribute(String s) {
        try {
            return ResourceStatus.valueOf(s);
        } catch (Exception e) {
            // If the value is not recognized, we default to USER_MANAGED
            return ResourceStatus.USER_MANAGED;
        }
    }
}
