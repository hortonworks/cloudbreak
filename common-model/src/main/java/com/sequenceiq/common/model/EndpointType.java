package com.sequenceiq.common.model;

public enum EndpointType {
    /*
    AWS:    do not use private endpoints
    AZURE:  invalid
     */
    NONE,

    /*
    AWS:    use private endpoints
    AZURE:  use service endpoints
     */
    USE_SERVICE_ENDPOINT,

    /*
    AWS:    invalid
    AZURE:  use private endpoints
     */
    USE_PRIVATE_ENDPOINT;

    public static EndpointType safeValueOf(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
