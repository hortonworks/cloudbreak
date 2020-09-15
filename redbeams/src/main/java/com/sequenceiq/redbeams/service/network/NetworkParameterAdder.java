package com.sequenceiq.redbeams.service.network;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.exception.RedbeamsException;

@Service
public class NetworkParameterAdder {

    // These constants must match those in AwsNetworkView

    @VisibleForTesting
    static final String VPC_ID = "vpcId";

    @VisibleForTesting
    static final String VPC_CIDR = "vpcCidr";

    @VisibleForTesting
    static final String VPC_CIDRS = "vpcCidrs";

    @VisibleForTesting
    static final String SUBNET_ID = "subnetId";

    @VisibleForTesting
    static final String AVAILABILITY_ZONE = "availabilityZone";

    // These constants must match those in AzureNetworkView

    @VisibleForTesting
    static final String SUBNETS = "subnets";

    public Map<String, Object> addSubnetIds(Map<String, Object> parameters, List<String> subnetIds, List<String> azs, CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS:
            case GCP:
            case MOCK:
                parameters.put(SUBNET_ID, String.join(",", subnetIds));
                parameters.put(AVAILABILITY_ZONE, String.join(",", azs));
                break;
            case AZURE:
                parameters.put(SUBNETS, String.join(",", subnetIds));
                break;
            default:
                throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
        return parameters;
    }

    public Map<String, Object> addParameters(Map<String, Object> parameters, DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS:
                parameters.put(VPC_CIDR, environmentResponse.getNetwork().getNetworkCidr());
                parameters.put(VPC_CIDRS, environmentResponse.getNetwork().getNetworkCidrs());
                parameters.put(VPC_ID, environmentResponse.getNetwork().getAws().getVpcId());
                break;
            case AZURE:
                // oddly, nothing to pass on yet
                //parameters.put(VPC_ID, environmentResponse.getNetwork().getAzure().getNetworkId());
                break;
            case GCP:
                // oddly, nothing to pass on yet
                break;
            case MOCK:
                parameters.put(VPC_ID, environmentResponse.getNetwork().getMock().getVpcId());
                break;
            default:
                throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
        return parameters;
    }
}
