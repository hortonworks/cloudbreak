package com.sequenceiq.common.model;

public enum PrivateEndpointType {
    /*
    AWS:    do not use vpc endpoints
    AZURE:  use service endpoints (https://docs.microsoft.com/en-us/azure/virtual-network/virtual-network-service-endpoints-overview)
     */
    NONE,

    /*
    AWS:    use vpc endpoints (https://docs.aws.amazon.com/vpc/latest/userguide/vpc-endpoints.html)
    AZURE:  invalid
     */
    USE_VPC_ENDPOINT,

    /*
    AWS:    invalid
    AZURE:  use private endpoints (https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview)
     */
    USE_PRIVATE_ENDPOINT;

    public static PrivateEndpointType safeValueOf(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return NONE;
        }
    }
}
