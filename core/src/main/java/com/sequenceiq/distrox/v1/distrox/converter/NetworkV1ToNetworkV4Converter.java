package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.evaluateIfTrueDoOtherwise;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.GcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.MockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.yarn.YarnNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class NetworkV1ToNetworkV4Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkV1ToNetworkV4Converter.class);

    @Inject
    private SubnetSelector subnetSelector;

    @Inject
    private EndpointGatewayNetworkValidator endpointGatewayNetworkValidator;

    public NetworkV4Request convertToNetworkV4Request(Pair<NetworkV1Request, DetailedEnvironmentResponse> network) {
        EnvironmentNetworkResponse value = network.getValue().getNetwork();
        NetworkV1Request key = network.getKey();
        if (key == null) {
            key = new NetworkV1Request();
        }

        NetworkV4Request request = new NetworkV4Request();

        if (network.getValue().getCloudPlatform() == null) {
            throw new IllegalStateException("Unable to determine cloud platform for network since it has not been set!");
        }
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
            default:
                LOGGER.warn(NetworkV1ToNetworkV4Converter.class.getSimpleName() + " has no implemented action for cloud platform: " +
                        network.getValue().getCloudPlatform());
        }
        return request;
    }

    private MockNetworkV4Parameters getMockNetworkParameters(Optional<MockNetworkV1Parameters> mockKey, EnvironmentNetworkResponse value) {
        MockNetworkV1Parameters params = mockKey.orElse(new MockNetworkV1Parameters());
        return convertToMockNetworkParams(new ImmutablePair<>(params, value));
    }

    private AzureNetworkV4Parameters getAzureNetworkParameters(Optional<AzureNetworkV1Parameters> azureKey, EnvironmentNetworkResponse value) {
        AzureNetworkV1Parameters params = azureKey.orElse(new AzureNetworkV1Parameters());
        return convertToAzureStackRequest(new ImmutablePair<>(params, value));
    }

    private AwsNetworkV4Parameters getAwsNetworkParameters(Optional<AwsNetworkV1Parameters> awsKey, EnvironmentNetworkResponse value) {
        AwsNetworkV1Parameters params = awsKey.orElse(new AwsNetworkV1Parameters());
        return convertToAwsStackRequest(new ImmutablePair<>(params, value));
    }

    private GcpNetworkV4Parameters getGcpNetworkParameters(Optional<GcpNetworkV1Parameters> gcpKey, EnvironmentNetworkResponse value) {
        GcpNetworkV1Parameters params = gcpKey.orElse(new GcpNetworkV1Parameters());
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
            } else if (value != null) {
                response.setSubnetId(value.getPreferedSubnetId());
            }

            if (PublicEndpointAccessGateway.ENABLED.equals(value.getPublicEndpointAccessGateway())) {
                ValidationResult validationResult = endpointGatewayNetworkValidator.validate(new ImmutablePair<>(response.getSubnetId(), value));
                if (validationResult.getState() == ValidationResult.State.ERROR || validationResult.hasError()) {
                    throw new BadRequestException("Endpoint gateway subnet validation failed: " + validationResult.getFormattedErrors());
                }
                Optional<CloudSubnet> endpointGatewaySubnet = subnetSelector.chooseSubnetForEndpointGateway(value, response.getSubnetId());
                if (endpointGatewaySubnet.isPresent()) {
                    response.setEndpointGatewaySubnetId(endpointGatewaySubnet.get().getId());
                }
            }
        }

        return response;
    }

    public NetworkV1Request convertToNetworkV1Request(NetworkV4Request network) {
        NetworkV1Request response = new NetworkV1Request();
        response.setAws(getIfNotNull(network.getAws(), this::convertToAwsNetworkV1Parameters));
        response.setAzure(getIfNotNull(network.getAzure(), this::convertToAzureNetworkV1Parameters));
        response.setGcp(getIfNotNull(network.getGcp(), this::convertToGcpNetworkV1Parameters));
        response.setYarn(getIfNotNull(network.getYarn(), this::convertToYarnNetworkV1Parameters));
        response.setMock(getIfNotNull(network.getMock(), this::convertToMockNetworkV1Parameters));
        return response;
    }

    private YarnNetworkV1Parameters convertToYarnNetworkV1Parameters(YarnNetworkV4Parameters source) {
        return new YarnNetworkV1Parameters();
    }

    private MockNetworkV1Parameters convertToMockNetworkV1Parameters(MockNetworkV4Parameters source) {
        MockNetworkV1Parameters response = new MockNetworkV1Parameters();
        response.setSubnetId(source.getSubnetId());
        response.setVpcId(source.getVpcId());
        response.setInternetGatewayId(source.getInternetGatewayId());
        return response;
    }

    private AzureNetworkV1Parameters convertToAzureNetworkV1Parameters(AzureNetworkV4Parameters source) {
        AzureNetworkV1Parameters response = new AzureNetworkV1Parameters();
        response.setSubnetId(source.getSubnetId());
        return response;
    }

    private GcpNetworkV1Parameters convertToGcpNetworkV1Parameters(GcpNetworkV4Parameters source) {
        GcpNetworkV1Parameters response = new GcpNetworkV1Parameters();
        response.setSubnetId(source.getSubnetId());
        return response;
    }

    private AwsNetworkV1Parameters convertToAwsNetworkV1Parameters(AwsNetworkV4Parameters source) {
        AwsNetworkV1Parameters response = new AwsNetworkV1Parameters();
        response.setSubnetId(source.getSubnetId());
        return response;
    }
}
