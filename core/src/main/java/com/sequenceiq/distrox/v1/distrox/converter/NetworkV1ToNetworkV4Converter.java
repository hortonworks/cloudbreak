package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperation;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class NetworkV1ToNetworkV4Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOperation.class);

    public NetworkV4Request convertToNetworkV4Request(Pair<NetworkV1Request, EnvironmentNetworkResponse> network) {
        EnvironmentNetworkResponse value = network.getValue();
        NetworkV1Request key = network.getKey();

        NetworkV4Request response = new NetworkV4Request();
        if (key != null) {
            response.setAws(getIfNotNull(new ImmutablePair<>(key.getAws(), value), this::convertToAwsStackRequest));
            response.setAzure(getIfNotNull(new ImmutablePair<>(key.getAzure(), value), this::convertToAzureStackRequest));
        }
        return response;
    }

    private AzureNetworkV4Parameters convertToAzureStackRequest(Pair<AzureNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        AzureNetworkV1Parameters key = source.getKey();

        AzureNetworkV4Parameters response = new AzureNetworkV4Parameters();

        if (key != null) {
            response.setNetworkId(value.getAzure().getNetworkId());
            response.setNoFirewallRules(value.getAzure().getNoFirewallRules());
            response.setNoPublicIp(value.getAzure().getNoPublicIp());
            response.setResourceGroupName(value.getAzure().getResourceGroupName());

            String subnetId = key.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(subnetId);
            } else {
                response.setSubnetId(source.getValue().getSubnetIds().stream().findFirst()
                        .orElseThrow(() -> new BadRequestException("No subnet id for this environment")));
            }
        }

        return response;
    }

    private AwsNetworkV4Parameters convertToAwsStackRequest(Pair<AwsNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        AwsNetworkV1Parameters key = source.getKey();

        AwsNetworkV4Parameters response = new AwsNetworkV4Parameters();

        if (key != null) {
            response.setVpcId(value.getAws().getVpcId());

            String subnetId = key.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(key.getSubnetId());
            } else {
                response.setSubnetId(value.getSubnetIds().stream().findFirst()
                        .orElseThrow(() -> new BadRequestException("No subnet id for this environment")));
            }
        }

        return response;
    }

    public NetworkV1Request convertToNetworkV1Request(NetworkV4Request network) {
        NetworkV1Request response = new NetworkV1Request();
        response.setAws(getIfNotNull(network.getAws(), this::convertToDistroXRequest));
        response.setAzure(getIfNotNull(network.getAzure(), this::convertToDistroXRequest));
        return response;
    }

    private AzureNetworkV1Parameters convertToDistroXRequest(AzureNetworkV4Parameters source) {
        AzureNetworkV1Parameters response = new AzureNetworkV1Parameters();
        response.setSubnetId(source.getSubnetId());
        return response;
    }

    private AwsNetworkV1Parameters convertToDistroXRequest(AwsNetworkV4Parameters source) {
        AwsNetworkV1Parameters response = new AwsNetworkV1Parameters();
        response.setSubnetId(source.getSubnetId());
        return response;
    }
}
