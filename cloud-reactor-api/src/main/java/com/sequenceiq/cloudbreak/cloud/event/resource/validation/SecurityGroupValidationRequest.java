package com.sequenceiq.cloudbreak.cloud.event.resource.validation;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

/**
 * Platform-agnostic request to verify that a set of provider-side security groups exist and (when a network ID is
 * provided) belong to that network. Handled by {@code SecurityGroupValidationHandler}. The caller decides whether a
 * network check is meaningful — AWS variant migration currently passes the environment VPC ID; other callers may
 * pass {@code null} to only verify existence.
 */
public class SecurityGroupValidationRequest extends CloudPlatformRequest<SecurityGroupValidationResult> {

    private final ExtendedCloudCredential extendedCloudCredential;

    private final String region;

    private final Set<String> securityGroupIds;

    private final String networkId;

    public SecurityGroupValidationRequest(CloudContext cloudContext, CloudCredential cloudCredential, ExtendedCloudCredential extendedCloudCredential,
            String region, Set<String> securityGroupIds, String networkId) {
        super(cloudContext, cloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
        this.region = region;
        this.securityGroupIds = securityGroupIds == null ? Set.of() : Set.copyOf(securityGroupIds);
        this.networkId = networkId;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public String getRegion() {
        return region;
    }

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public String getNetworkId() {
        return networkId;
    }

    @Override
    public String toString() {
        return "SecurityGroupValidationRequest{"
                + "securityGroupIds=" + securityGroupIds
                + ", networkId='" + networkId + '\''
                + ", region='" + region + '\''
                + '}';
    }
}
