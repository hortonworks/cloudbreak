package com.sequenceiq.redbeams.service.network;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.exception.RedbeamsException;

@Service
public class NetworkParameterAdder {

    private static final String VPC_ID = "vpcId";

    private static final String VPC_CIDR = "vpcCidr";

    private static final String SUBNET_ID = "subnetId";

    public Map<String, Object> addNetworkParameters(Map<String, Object> parameters, List<String> subnetIds) {
        parameters.put(SUBNET_ID, String.join(",", subnetIds));
        return parameters;
    }

    public Map<String, Object> addVpcParameters(Map<String, Object> parameters, DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform) {
        parameters.put(VPC_CIDR, environmentResponse.getSecurityAccess().getCidr());
        parameters.put(VPC_ID, getVpcId(environmentResponse, cloudPlatform));
        return parameters;
    }

    private String getVpcId(DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS:
                return environmentResponse.getNetwork().getAws().getVpcId();
            case AZURE:
                return environmentResponse.getNetwork().getAzure().getNetworkId();
            default:
                throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
    }
}
