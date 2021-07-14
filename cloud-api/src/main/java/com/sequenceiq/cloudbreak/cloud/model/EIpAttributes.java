package com.sequenceiq.cloudbreak.cloud.model;


import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EIpAttributes {

    private String associationId;

    private String allocateId;

    private Class<EIpAttributes> attributeType = EIpAttributes.class;

    public EIpAttributes() {
    }

    public EIpAttributes(String associationId, String allocateId) {
        this.associationId = associationId;
        this.allocateId = allocateId;
    }

    public String getAssociationId() {
        return associationId;
    }

    public void setAssociationId(String associationId) {
        this.associationId = associationId;
    }

    public String getAllocateId() {
        return allocateId;
    }

    public void setAllocateId(String allocateId) {
        this.allocateId = allocateId;
    }

    public Class<EIpAttributes> getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(Class<EIpAttributes> attributeType) {
        this.attributeType = attributeType;
    }

    public static final class EIpAttributesBuilder {
        private String associationId;

        private String allocateId;

        private EIpAttributesBuilder() {
        }

        public static EIpAttributesBuilder builder() {
            return new EIpAttributesBuilder();
        }

        public EIpAttributesBuilder withAssociationId(String associationId) {
            this.associationId = associationId;
            return this;
        }

        public EIpAttributesBuilder withAllocateId(String allocateId) {
            this.allocateId = allocateId;
            return this;
        }

        public EIpAttributes build() {
            return new EIpAttributes(associationId, allocateId);
        }
    }
}
