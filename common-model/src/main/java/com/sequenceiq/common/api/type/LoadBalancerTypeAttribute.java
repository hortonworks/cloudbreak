package com.sequenceiq.common.api.type;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reason of existence of this enum is that it needs to be serialized as an object in CloudResource attribute.
 * The {@link LoadBalancerType} enum is used at many places and the serialization format could not be changed.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LoadBalancerTypeAttribute {
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

    private static final String NAME_KEY = "name";

    private static final String ATTRIBUTE_TYPE_KEY = "attributeType";

    private final Class<LoadBalancerTypeAttribute> attributeType = LoadBalancerTypeAttribute.class;

    /**
     * Needed for serialization
     * @return name of the enum
     */
    @JsonProperty(NAME_KEY)
    public String getName() {
        return name();
    }

    /**
     * Needed for serialization
     * @return class of the current enum
     */
    @JsonProperty(ATTRIBUTE_TYPE_KEY)
    public Class<LoadBalancerTypeAttribute> getAttributeType() {
        return attributeType;
    }

    /**
     * Factory method to create enum instance from JSON object (map-based)
     * @param value map containing enum properties
     * @return enum instance
     */
    @JsonCreator
    public static LoadBalancerTypeAttribute fromMap(Map<String, Object> value) {
        Object nameObj = value.get(NAME_KEY);
        if (nameObj instanceof String) {
            return LoadBalancerTypeAttribute.valueOf((String) nameObj);
        }
        throw new IllegalArgumentException("Cannot deserialize LoadBalancerTypeAttribute from: " + value);
    }

    public Map<String, Object> asMap() {
        return Map.of(NAME_KEY, name(),
                ATTRIBUTE_TYPE_KEY, attributeType);
    }
}