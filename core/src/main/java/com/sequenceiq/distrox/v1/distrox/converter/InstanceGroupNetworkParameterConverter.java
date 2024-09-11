package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.NetworkV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.azure.InstanceGroupAzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.gcp.InstanceGroupGcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.mock.InstanceGroupMockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.yarn.InstanceGroupYarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.InstanceGroupAzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.InstanceGroupGcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.InstanceGroupMockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.yarn.InstanceGroupYarnNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class InstanceGroupNetworkParameterConverter {

    public InstanceGroupMockNetworkV4Parameters convert(InstanceGroupMockNetworkV1Parameters mock, EnvironmentNetworkResponse value,
            CloudPlatform cloudPlatform, NetworkV4Request stackLevelNetwork) {
        InstanceGroupMockNetworkV4Parameters response = null;
        if (CloudPlatform.MOCK == cloudPlatform) {
            InstanceGroupMockNetworkV1Parameters params = mock == null
                    ? new InstanceGroupMockNetworkV1Parameters() : mock;
            response = convertToMockNetworkParams(params, value, stackLevelNetwork);
        }
        return response;
    }

    public InstanceGroupAzureNetworkV4Parameters convert(InstanceGroupAzureNetworkV1Parameters azure, EnvironmentNetworkResponse value,
            CloudPlatform cloudPlatform, NetworkV4Request stackLevelNetwork) {
        InstanceGroupAzureNetworkV4Parameters response = null;
        if (CloudPlatform.AZURE == cloudPlatform) {
            InstanceGroupAzureNetworkV1Parameters params = azure == null
                    ? new InstanceGroupAzureNetworkV1Parameters() : azure;
            response = convertToAzureNetworkParams(params, value, stackLevelNetwork);
        }
        return response;
    }

    public InstanceGroupAwsNetworkV4Parameters convert(InstanceGroupAwsNetworkV1Parameters aws, EnvironmentNetworkResponse value, CloudPlatform cloudPlatform,
            NetworkV4Request stackLevelNetwork) {
        InstanceGroupAwsNetworkV4Parameters response = null;
        if (CloudPlatform.AWS == cloudPlatform) {
            InstanceGroupAwsNetworkV1Parameters params = aws == null ?
                    new InstanceGroupAwsNetworkV1Parameters() : aws;
            response = convertToAwsNetworkParams(params, value, stackLevelNetwork);
        }
        return response;
    }

    public InstanceGroupGcpNetworkV4Parameters convert(InstanceGroupGcpNetworkV1Parameters gcp, EnvironmentNetworkResponse value, CloudPlatform cloudPlatform,
            NetworkV4Request stackLevelNetwork) {
        InstanceGroupGcpNetworkV4Parameters response = null;
        if (CloudPlatform.GCP == cloudPlatform) {
            InstanceGroupGcpNetworkV1Parameters params = gcp == null
                    ? new InstanceGroupGcpNetworkV1Parameters() : gcp;
            response = convertToGcpNetworkParams(params, value, stackLevelNetwork);
        }
        return response;
    }

    public InstanceGroupYarnNetworkV4Parameters convert(InstanceGroupYarnNetworkV1Parameters yarn,
        EnvironmentNetworkResponse value, CloudPlatform cloudPlatform) {
        InstanceGroupYarnNetworkV4Parameters response = null;
        if (CloudPlatform.YARN == cloudPlatform) {
            InstanceGroupYarnNetworkV1Parameters params = yarn == null
                    ? new InstanceGroupYarnNetworkV1Parameters() : yarn;
            response = convertToYarnNetworkParams(new ImmutablePair<>(params, value));
        }
        return response;
    }

    private InstanceGroupMockNetworkV4Parameters convertToMockNetworkParams(InstanceGroupMockNetworkV1Parameters instanceGroupMockNetworkV1Parameters,
            EnvironmentNetworkResponse environmentNetworkResponse, NetworkV4Request stackLevelNetwork) {
        InstanceGroupMockNetworkV4Parameters params = new InstanceGroupMockNetworkV4Parameters();

        if (instanceGroupMockNetworkV1Parameters != null) {
            List<String> subnetIds = instanceGroupMockNetworkV1Parameters.getSubnetIds();
            if (environmentNetworkResponse != null) {
                if (CollectionUtils.isNotEmpty(subnetIds)) {
                    params.setSubnetIds(subnetIds);
                } else {
                    String stackLevelSubnetId = Optional.ofNullable(stackLevelNetwork)
                            .map(NetworkV4Base::getMock)
                            .map(MockNetworkV4Parameters::getSubnetId)
                            .orElse(null);
                    setFallbackSubnetIdForInstanceGroupNetwork(stackLevelSubnetId, environmentNetworkResponse, params::setSubnetIds);
                }
            }
        }

        return params;
    }

    private InstanceGroupAzureNetworkV4Parameters convertToAzureNetworkParams(InstanceGroupAzureNetworkV1Parameters instanceGroupAzureNetworkV1Parameters,
            EnvironmentNetworkResponse environmentNetworkResponse, NetworkV4Request stackLevelNetwork) {
        InstanceGroupAzureNetworkV4Parameters response = new InstanceGroupAzureNetworkV4Parameters();

        if (instanceGroupAzureNetworkV1Parameters != null) {
            List<String> subnetIds = instanceGroupAzureNetworkV1Parameters.getSubnetIds();
            if (CollectionUtils.isNotEmpty(subnetIds)) {
                response.setSubnetIds(subnetIds);
            } else {
                String stackLevelSubnetId = Optional.ofNullable(stackLevelNetwork)
                        .map(NetworkV4Base::getAzure)
                        .map(AzureNetworkV4Parameters::getSubnetId)
                        .orElse(null);
                setFallbackSubnetIdForInstanceGroupNetwork(stackLevelSubnetId, environmentNetworkResponse, response::setSubnetIds);
            }
            Set<String> availabilityZones = instanceGroupAzureNetworkV1Parameters.getAvailabilityZones();
            if (CollectionUtils.isNotEmpty(availabilityZones)) {
                response.setAvailabilityZones(availabilityZones);
            }
        }
        return response;
    }

    private InstanceGroupGcpNetworkV4Parameters convertToGcpNetworkParams(InstanceGroupGcpNetworkV1Parameters groupGcpNetworkV1Parameters,
            EnvironmentNetworkResponse envNetwork, NetworkV4Request stackLevelNetwork) {
        InstanceGroupGcpNetworkV4Parameters response = new InstanceGroupGcpNetworkV4Parameters();

        if (groupGcpNetworkV1Parameters != null) {
            List<String> subnetIds = groupGcpNetworkV1Parameters.getSubnetIds();
            if (CollectionUtils.isNotEmpty(subnetIds)) {
                response.setSubnetIds(subnetIds);
            } else {
                String stackLevelSubnetId = Optional.ofNullable(stackLevelNetwork)
                        .map(NetworkV4Base::getGcp)
                        .map(GcpNetworkV4Parameters::getSubnetId)
                        .orElse(null);
                setFallbackSubnetIdForInstanceGroupNetwork(stackLevelSubnetId, envNetwork, response::setSubnetIds);
            }
        }

        return response;
    }

    private InstanceGroupYarnNetworkV4Parameters convertToYarnNetworkParams(Pair<InstanceGroupYarnNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        return new InstanceGroupYarnNetworkV4Parameters();
    }

    private InstanceGroupAwsNetworkV4Parameters convertToAwsNetworkParams(InstanceGroupAwsNetworkV1Parameters instanceGroupAwsNetworkV1Parameters,
            EnvironmentNetworkResponse environmentNetworkResponse, NetworkV4Request stackLevelNetwork) {
        InstanceGroupAwsNetworkV4Parameters response = new InstanceGroupAwsNetworkV4Parameters();

        if (instanceGroupAwsNetworkV1Parameters != null) {
            List<String> subnetIds = instanceGroupAwsNetworkV1Parameters.getSubnetIds();
            if (CollectionUtils.isNotEmpty(subnetIds)) {
                response.setSubnetIds(subnetIds);
            } else {
                String stackLevelSubnetId = Optional.ofNullable(stackLevelNetwork)
                        .map(NetworkV4Base::getAws)
                        .map(AwsNetworkV4Parameters::getSubnetId)
                        .orElse(null);
                setFallbackSubnetIdForInstanceGroupNetwork(stackLevelSubnetId, environmentNetworkResponse, response::setSubnetIds);
            }
            List<String> endpointGatewaySubnetIds = instanceGroupAwsNetworkV1Parameters.getEndpointGatewaySubnetIds();
            if (CollectionUtils.isNotEmpty(endpointGatewaySubnetIds)) {
                response.setEndpointGatewaySubnetIds(endpointGatewaySubnetIds);
            } else if (environmentNetworkResponse != null && CollectionUtils.isNotEmpty(environmentNetworkResponse.getEndpointGatewaySubnetIds())) {
                response.setEndpointGatewaySubnetIds(List.copyOf(environmentNetworkResponse.getEndpointGatewaySubnetIds()));
            }
        }

        return response;
    }

    private void setFallbackSubnetIdForInstanceGroupNetwork(String stackLevelNetwork, EnvironmentNetworkResponse envNetwork,
            Consumer<List<String>> setter) {
        String fallbackSubnetId = getSubnetIdFromStackOrEnvironmentLevel(stackLevelNetwork, envNetwork);
        if (StringUtils.isNotEmpty(fallbackSubnetId)) {
            setter.accept(List.of(fallbackSubnetId));
        }
    }

    private String getSubnetIdFromStackOrEnvironmentLevel(String stackLevelSubnetId, EnvironmentNetworkResponse environmentNetworkResponse) {
        String fallbackSubnet = null;
        if (StringUtils.isNotEmpty(stackLevelSubnetId)) {
            fallbackSubnet = stackLevelSubnetId;
        } else if (environmentNetworkResponse != null && StringUtils.isNotEmpty(environmentNetworkResponse.getPreferedSubnetId())) {
            fallbackSubnet = environmentNetworkResponse.getPreferedSubnetId();
        } else {
            throw new BadRequestException("Subnet could not be selected for you request, please check your environment and it's network configuration.");
        }
        return fallbackSubnet;
    }
}