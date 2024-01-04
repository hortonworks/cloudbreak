package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.evaluateIfTrueDoOtherwise;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class NetworkV1ToNetworkV4Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkV1ToNetworkV4Converter.class);

    @Inject
    private SubnetSelector subnetSelector;

    @Inject
    private EndpointGatewayNetworkValidator endpointGatewayNetworkValidator;

    @Inject
    private EntitlementService entitlementService;

    public NetworkV4Request convertToNetworkV4Request(Pair<NetworkV1Request, DetailedEnvironmentResponse> source) {
        EnvironmentNetworkResponse networkResponse = source.getValue().getNetwork();
        NetworkV1Request networkV1Request = source.getKey();
        if (networkV1Request == null) {
            networkV1Request = new NetworkV1Request();
        }

        NetworkV4Request request = new NetworkV4Request();

        if (source.getValue().getCloudPlatform() == null) {
            throw new IllegalStateException("Unable to determine cloud platform for network since it has not been set!");
        }
        switch (source.getValue().getCloudPlatform()) {
            case "AWS":
                request.setAws(getAwsNetworkParameters(Optional.ofNullable(networkV1Request.getAws()), networkResponse));
                break;
            case "AZURE":
                request.setAzure(getAzureNetworkParameters(Optional.ofNullable(networkV1Request.getAzure()), networkResponse));
                break;
            case "MOCK":
                request.setMock(getMockNetworkParameters(Optional.ofNullable(networkV1Request.getMock()), networkResponse));
                break;
            case "GCP":
                request.setGcp(getGcpNetworkParameters(Optional.ofNullable(networkV1Request.getGcp()), networkResponse));
                break;
            default:
                LOGGER.warn(NetworkV1ToNetworkV4Converter.class.getSimpleName() + " has no implemented action for cloud platform: " +
                        source.getValue().getCloudPlatform());
        }
        return request;
    }

    private MockNetworkV4Parameters getMockNetworkParameters(Optional<MockNetworkV1Parameters> mockKey, EnvironmentNetworkResponse networkResponse) {
        MockNetworkV1Parameters params = mockKey.orElse(new MockNetworkV1Parameters());
        return convertToMockNetworkParams(new ImmutablePair<>(params, networkResponse));
    }

    private AzureNetworkV4Parameters getAzureNetworkParameters(Optional<AzureNetworkV1Parameters> azureKey, EnvironmentNetworkResponse networkResponse) {
        AzureNetworkV1Parameters params = azureKey.orElse(new AzureNetworkV1Parameters());
        return convertToAzureStackRequest(new ImmutablePair<>(params, networkResponse));
    }

    private AwsNetworkV4Parameters getAwsNetworkParameters(Optional<AwsNetworkV1Parameters> awsKey, EnvironmentNetworkResponse networkResponse) {
        AwsNetworkV1Parameters params = awsKey.orElse(new AwsNetworkV1Parameters());
        return convertToAwsStackRequest(new ImmutablePair<>(params, networkResponse));
    }

    private GcpNetworkV4Parameters getGcpNetworkParameters(Optional<GcpNetworkV1Parameters> gcpKey, EnvironmentNetworkResponse networkResponse) {
        GcpNetworkV1Parameters params = gcpKey.orElse(new GcpNetworkV1Parameters());
        return convertToGcpStackRequest(new ImmutablePair<>(params, networkResponse));
    }

    private MockNetworkV4Parameters convertToMockNetworkParams(Pair<MockNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse networkResponse = source.getValue();
        MockNetworkV1Parameters mockNetworkV1Parameters = source.getKey();

        MockNetworkV4Parameters params = new MockNetworkV4Parameters();

        if (mockNetworkV1Parameters != null) {
            String subnetId = mockNetworkV1Parameters.getSubnetId();
            if (networkResponse != null) {
                evaluateIfTrueDoOtherwise(subnetId, StringUtils::isNotEmpty, params::setSubnetId,
                        s -> params.setSubnetId(networkResponse.getPreferedSubnetId()));
            }
            params.setInternetGatewayId(mockNetworkV1Parameters.getInternetGatewayId());
            params.setVpcId(mockNetworkV1Parameters.getVpcId());
        }

        return params;
    }

    private AzureNetworkV4Parameters convertToAzureStackRequest(Pair<AzureNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse networkResponse = source.getValue();
        AzureNetworkV1Parameters azureNetworkV1Parameters = source.getKey();

        AzureNetworkV4Parameters response = new AzureNetworkV4Parameters();

        if (azureNetworkV1Parameters != null) {
            EnvironmentNetworkAzureParams networkAzureParams = networkResponse.getAzure();
            response.setNetworkId(networkAzureParams.getNetworkId());
            response.setNoPublicIp(networkAzureParams.getNoPublicIp());
            response.setResourceGroupName(networkAzureParams.getResourceGroupName());
            response.setDatabasePrivateDnsZoneId(networkAzureParams.getDatabasePrivateDnsZoneId());
            response.setAksPrivateDnsZoneId(networkAzureParams.getAksPrivateDnsZoneId());
            response.setNoOutboundLoadBalancer(BooleanUtils.toBooleanDefaultIfNull(networkAzureParams.getNoOutboundLoadBalancer(),
                    networkResponse.isExistingNetwork()));
            String subnetId = azureNetworkV1Parameters.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(subnetId);
            } else if (source.getValue() != null) {
                response.setSubnetId(source.getValue().getPreferedSubnetId());
            }
        }

        return response;
    }

    private GcpNetworkV4Parameters convertToGcpStackRequest(Pair<GcpNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse networkResponse = source.getValue();
        GcpNetworkV1Parameters gcpNetworkV1Parameters = source.getKey();

        GcpNetworkV4Parameters response = new GcpNetworkV4Parameters();

        if (gcpNetworkV1Parameters != null) {
            response.setNetworkId(networkResponse.getGcp().getNetworkId());
            response.setNoFirewallRules(networkResponse.getGcp().getNoFirewallRules());
            response.setNoPublicIp(networkResponse.getGcp().getNoPublicIp());
            response.setSharedProjectId(networkResponse.getGcp().getSharedProjectId());

            String subnetId = gcpNetworkV1Parameters.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(subnetId);
            } else {
                response.setSubnetId(getFirstSubnetIdFromEnvironment(networkResponse));
            }
        }

        return response;
    }

    private String getFirstSubnetIdFromEnvironment(EnvironmentNetworkResponse enr) {
        return enr.getSubnetIds().stream().findFirst().orElseThrow(() -> new BadRequestException("No subnet id for this environment"));
    }

    private AwsNetworkV4Parameters convertToAwsStackRequest(Pair<AwsNetworkV1Parameters, EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse networkResponse = source.getValue();
        AwsNetworkV1Parameters awsNetworkV1Parameters = source.getKey();

        AwsNetworkV4Parameters response = new AwsNetworkV4Parameters();

        if (awsNetworkV1Parameters != null) {
            response.setVpcId(networkResponse.getAws().getVpcId());

            String subnetId = awsNetworkV1Parameters.getSubnetId();
            if (!Strings.isNullOrEmpty(subnetId)) {
                response.setSubnetId(awsNetworkV1Parameters.getSubnetId());
            } else {
                response.setSubnetId(networkResponse.getPreferedSubnetId());
            }

            if (PublicEndpointAccessGateway.ENABLED.equals(networkResponse.getPublicEndpointAccessGateway()) || isTargetingEndpointGateway(networkResponse)) {
                ValidationResult validationResult = endpointGatewayNetworkValidator.validate(new ImmutablePair<>(response.getSubnetId(), networkResponse));
                if (validationResult.getState() == ValidationResult.State.ERROR || validationResult.hasError()) {
                    throw new BadRequestException("Endpoint gateway subnet validation failed: " + validationResult.getFormattedErrors());
                }
                Optional<CloudSubnet> endpointGatewaySubnet = subnetSelector.chooseSubnetForEndpointGateway(networkResponse, response.getSubnetId());
                if (endpointGatewaySubnet.isPresent()) {
                    response.setEndpointGatewaySubnetId(endpointGatewaySubnet.get().getId());
                }
            }
        }

        return response;
    }

    private boolean isTargetingEndpointGateway(EnvironmentNetworkResponse network) {
        return entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId()) &&
                CollectionUtils.isNotEmpty(network.getEndpointGatewaySubnetIds());
    }

    public NetworkV1Request convertToNetworkV1Request(NetworkV4Request network) {
        NetworkV1Request request = new NetworkV1Request();
        request.setAws(getIfNotNull(network.getAws(), this::convertToAwsNetworkV1Parameters));
        request.setAzure(getIfNotNull(network.getAzure(), this::convertToAzureNetworkV1Parameters));
        request.setGcp(getIfNotNull(network.getGcp(), this::convertToGcpNetworkV1Parameters));
        request.setYarn(getIfNotNull(network.getYarn(), this::convertToYarnNetworkV1Parameters));
        request.setMock(getIfNotNull(network.getMock(), this::convertToMockNetworkV1Parameters));
        return request;
    }

    private YarnNetworkV1Parameters convertToYarnNetworkV1Parameters(YarnNetworkV4Parameters source) {
        return new YarnNetworkV1Parameters();
    }

    private MockNetworkV1Parameters convertToMockNetworkV1Parameters(MockNetworkV4Parameters source) {
        MockNetworkV1Parameters parameters = new MockNetworkV1Parameters();
        parameters.setSubnetId(source.getSubnetId());
        parameters.setVpcId(source.getVpcId());
        parameters.setInternetGatewayId(source.getInternetGatewayId());
        return parameters;
    }

    private AzureNetworkV1Parameters convertToAzureNetworkV1Parameters(AzureNetworkV4Parameters source) {
        AzureNetworkV1Parameters parameters = new AzureNetworkV1Parameters();
        parameters.setSubnetId(source.getSubnetId());
        return parameters;
    }

    private GcpNetworkV1Parameters convertToGcpNetworkV1Parameters(GcpNetworkV4Parameters source) {
        GcpNetworkV1Parameters parameters = new GcpNetworkV1Parameters();
        parameters.setSubnetId(source.getSubnetId());
        return parameters;
    }

    private AwsNetworkV1Parameters convertToAwsNetworkV1Parameters(AwsNetworkV4Parameters source) {
        AwsNetworkV1Parameters parameters = new AwsNetworkV1Parameters();
        parameters.setSubnetId(source.getSubnetId());
        return parameters;
    }
}
