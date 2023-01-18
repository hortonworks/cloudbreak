package com.sequenceiq.common.api.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LoadBalancerType {
    /**
     * Internet-facing public Load Balancer for the Gateway (CM/Knox)
     */
    PUBLIC,

    /**
     * Internal Load Balancer for CDP Runtime services, e.g. Oozie
     */
    PRIVATE,

    /**
     * Private Load Balancer for the Gateway (CM/Knox)
     */
    GATEWAY_PRIVATE,

    /**
     * Azure-specific Load Balancer type
     */
    OUTBOUND;

    private final Class<LoadBalancerType> attributeType = LoadBalancerType.class;

    /**
     * Needed for serialization
     * @return name of the enum
     */
    public String getName() {
        return name();
    }

    /**
     * Needed for serialization
     * @return class of the current enum
     */
    public Class<LoadBalancerType> getAttributeType() {
        return attributeType;
    }

    /**
     * Needed for deserialization
     * @param name String representation of the enum to be deserialized
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static LoadBalancerType create(@JsonProperty("name") String name) {
        return valueOf(name);
    }
}
