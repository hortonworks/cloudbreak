package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.evaluateIfTrueDoOtherwise;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.GcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.MockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.OpenstackNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class NetworkV1ToNetworkV4Converter {

    private static final String NO_SUBNET_ID_FOUND_MESSAGE = "No subnet id found for this environment.";

    public NetworkV4Request convertToNetworkV4Request(Pair<NetworkV1Request, DetailedEnvironmentResponse> network) {
        EnvironmentNetworkResponse value = network.getValue().getNetwork();
        NetworkV1Request key = network.getKey();
        if (key == null) {
            key = new NetworkV1Request();
        }

        NetworkV4Request request = new NetworkV4Request();

        switch (network.getValue().getCloudPlatform()) {
            case "AWS":
                request.setAws(getAwsNetworkParameters(Optional.ofNullable(key.getAws()), value));
                break;
            case "AZURE":
                request.setAzure(getAzureNetworkParameters(Optional.ofNullable(key.getAzure()), value));
                break;
            case "MOCK":
                request.setMock(getMockNetworkParameters(Optional.ofNullable(key.getMock()), value));
                break;
            case "GCP":
                request.setGcp(getGcpNetworkParameters(Optional.ofNullable(key.getGcp()), value));
                break;
            case "OPENSTACK":
                request.setOpenstack(getOpenstackNetworkParameters(Optional.ofNullable(key.getOpenstack()), value));
                break;
            default:
        }
        return request;
    }

    private MockNetworkV4Parameters getMockNetworkParameters(Optional<MockNetworkV1Parameters> mock, EnvironmentNetworkResponse value) {
        MockNetworkV1Parameters params = mock.orElse(new MockNetworkV1Parameters());
        return convertToMockNetworkParams(new ImmutablePair<>(params, value));
    }

    private AzureNetworkV4Parameters getAzureNetworkParameters(Optional<AzureNetworkV1Parameters> azure, EnvironmentNetworkResponse value) {
        AzureNetworkV1Parameters params = azure.orElse(new AzureNetworkV1Parameters());
        return convertToAzureStackRequest(new ImmutablePair<>(params, value));
    }

    private AwsNetworkV4Parameters getAwsNetworkParameters(Optional<AwsNetworkV1Parameters> key, EnvironmentNetworkResponse value) {
        AwsNetworkV1Parameters params = key.orElse(new AwsNetworkV1Parameters());
        return convertToAwsStackRequest(new ImmutablePair<>(params, value));
    }

    private OpenStackNetworkV4Parameters getOpenstackNetworkParameters(Optional<OpenstackNetworkV1Parameters> mock, EnvironmentNetworkResponse value) {
        OpenstackNetworkV1Parameters params = mock.orElse(new OpenstackNetworkV1Parameters());
        return convertToOpenstackStackRequest(new ImmutablePair<>(params, value));
    }

    private GcpNetworkV4Parameters getGcpNetworkParameters(Optional<GcpNetworkV1Parameters> mock, EnvironmentNetworkResponse value) {
        GcpNetworkV1Parameters params = mock.orElse(new GcpNetworkV1Parameters());
        return convertToGcpStackRequest(new ImmutablePair<>(params, value));
    }

    private MockNetworkV4Parameters convertToMockNetworkParams(Pair<MockNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        MockNetworkV1Parameters key = source.getKey();

        MockNetworkV4Parameters params = new MockNetworkV4Parameters();

        if (key != null) {
            String subnetId = key.getSubnetId();
            if (value != null) {
                evaluateIfTrueDoOtherwise(subnetId, StringUtils::isNotEmpty, params::setSubnetId,
                        s -> params.setSubnetId(value.getPreferedSubnetId()));
            }
            params.setInternetGatewayId(key.getInternetGatewayId());
            params.setVpcId(key.getVpcId());
        }

        return params;
    }

    private AzureNetworkV4Parameters convertToAzureStackRequest(Pair<AzureNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        AzureNetworkV1Parameters key = source.getKey();

        AzureNetworkV4Parameters response = new AzureNetworkV4Parameters();

        if (key != null) {
            response.setNetworkId(value.getAzure().getNetworkId());
            response.setNoPublicIp(value.getAzure().getNoPublicIp());
            response.setResourceGroupName(value.getAzure().getResourceGroupName());

            String subnetId = key.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(subnetId);
            } else if (source.getValue() != null) {
                response.setSubnetId(source.getValue().getPreferedSubnetId());
            }
        }

        return response;
    }

    private OpenStackNetworkV4Parameters convertToOpenstackStackRequest(Pair<OpenstackNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        OpenstackNetworkV1Parameters key = source.getKey();

        OpenStackNetworkV4Parameters response = new OpenStackNetworkV4Parameters();

        if (key != null) {
            response.setNetworkId(value.getOpenstack().getNetworkId());
            response.setNetworkingOption(value.getOpenstack().getNetworkingOption());
            response.setPublicNetId(value.getOpenstack().getPublicNetId());
            response.setRouterId(value.getOpenstack().getRouterId());

            String subnetId = key.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(subnetId);
            } else {
                response.setSubnetId(getFirstSubnetIdFromEnvironment(value));
            }
        }

        return response;
    }

    private GcpNetworkV4Parameters convertToGcpStackRequest(Pair<GcpNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        GcpNetworkV1Parameters key = source.getKey();

        GcpNetworkV4Parameters response = new GcpNetworkV4Parameters();

        if (key != null) {
            response.setNetworkId(value.getGcp().getNetworkId());
            response.setNoFirewallRules(value.getGcp().getNoFirewallRules());
            response.setNoPublicIp(value.getGcp().getNoPublicIp());
            response.setSharedProjectId(value.getGcp().getSharedProjectId());

            String subnetId = key.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(subnetId);
            } else {
                response.setSubnetId(getFirstSubnetIdFromEnvironment(value));
            }
        }

        return response;
    }

    private String getFirstSubnetIdFromEnvironment(EnvironmentNetworkResponse enr) {
        return enr.getSubnetIds().stream().findFirst().orElseThrow(() -> new BadRequestException("No subnet id for this environment"));
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
            } else if (source.getValue() != null) {
                response.setSubnetId(source.getValue().getPreferedSubnetId());
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
