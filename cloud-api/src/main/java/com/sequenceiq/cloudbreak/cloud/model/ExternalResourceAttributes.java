package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalResourceAttributes implements Serializable {

    private final Class<ExternalResourceAttributes> attributeType = ExternalResourceAttributes.class;

    /**
     * Needed for serialization
     * @return class of the current enum
     */
    public Class<ExternalResourceAttributes> getAttributeType() {
        return attributeType;
    }

    /**
     * Predicate to filter out CloudResources that have ExternalResourceAttributes.
     * This is used during stack termination to exclude external resources from termination.
     *
     * @param cloudResource the CloudResource to check
     * @return true if the resource has ExternalResourceAttributes
     */
    public static boolean isExternalResource(CloudResource cloudResource) {
        String attributeType = cloudResource.getParameter(CloudResource.ATTRIBUTE_TYPE, String.class);
        return ExternalResourceAttributes.class.getCanonicalName().equals(attributeType);
    }

    @Override
    public String toString() {
        return "ExternalResourceAttributes{" +
                "attributeType=" + attributeType +
                '}';
    }
}