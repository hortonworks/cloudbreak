package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    @Override
    public String toString() {
        return "ExternalResourceAttributes{" +
                "attributeType=" + attributeType +
                '}';
    }
}