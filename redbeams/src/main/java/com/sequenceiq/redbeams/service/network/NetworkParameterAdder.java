package com.sequenceiq.redbeams.service.network;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Service
public class NetworkParameterAdder {

    private static final String VPC_ID = "vpcId";

    private static final String VPC_CIDR = "vpcCidr";

    private static final String SUBNET_ID = "subnetId";

    public Map<String, Object> addNetworkParameters(Map<String, Object> parameters, List<String> subnetIds) {
        parameters.put(SUBNET_ID, String.join(",", subnetIds));
        return parameters;
    }

    public Map<String, Object> addVpcParameters(Map<String, Object> parameters, EnvironmentNetworkResponse environmentNetworkResponse) {
        parameters.put(VPC_ID, String.join(",", environmentNetworkResponse.getAws().getVpcId()));
        parameters.put(VPC_CIDR, environmentNetworkResponse.getNetworkCidr());
        return parameters;
    }

}
