package com.sequenceiq.redbeams.service.network;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.AwsNetworkV4Parameters;

@Service
public class NetworkParameterFactoryService {

    public MappableBase createNetworkParameters(List<String> subnetIds, String cloudPlatform) {
        switch (cloudPlatform) {
            case "AWS":
                AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
                awsNetworkV4Parameters.setSubnetId(String.join(",", subnetIds));
                return awsNetworkV4Parameters;
            default:
                throw new BadRequestException(String.format("Cloud provider %s not supported yet.", cloudPlatform));
        }
    }
}
