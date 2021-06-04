package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.azure.InstanceGroupAzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.gcp.InstanceGroupGcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.mock.InstanceGroupMockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.openstack.InstanceGroupOpenstackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.InstanceGroupAzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.InstanceGroupGcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.InstanceGroupMockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.openstack.InstanceGroupOpenstackNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class InstanceGroupNetworkV1ToInstanceGroupNetworkV4Converter {

    public InstanceGroupNetworkV4Request convertToInstanceGroupNetworkV4Request(Pair<InstanceGroupNetworkV1Request, DetailedEnvironmentResponse> network) {
        DetailedEnvironmentResponse value = network.getValue();
        EnvironmentNetworkResponse environmentNetworkResponse = null;
        if (value == null) {
            environmentNetworkResponse = new EnvironmentNetworkResponse();
        } else {
            environmentNetworkResponse = value.getNetwork();
        }
        InstanceGroupNetworkV1Request key = network.getKey();
        if (key == null) {
            key = new InstanceGroupNetworkV1Request();
        }

        InstanceGroupNetworkV4Request request = new InstanceGroupNetworkV4Request();

        if (network.getValue() != null) {
            switch (value.getCloudPlatform()) {
                case "AWS":
                    request.setAws(getAwsNetworkParameters(Optional.ofNullable(key.getAws()), environmentNetworkResponse));
                    request.setCloudPlatform(CloudPlatform.AWS);
                    break;
                case "AZURE":
                    request.setAzure(getAzureNetworkParameters(Optional.ofNullable(key.getAzure()), environmentNetworkResponse));
                    request.setCloudPlatform(CloudPlatform.AZURE);
                    break;
                case "MOCK":
                    request.setMock(getMockNetworkParameters(Optional.ofNullable(key.getMock()), environmentNetworkResponse));
                    request.setCloudPlatform(CloudPlatform.MOCK);
                    break;
                case "GCP":
                    request.setGcp(getGcpNetworkParameters(Optional.ofNullable(key.getGcp()), environmentNetworkResponse));
                    request.setCloudPlatform(CloudPlatform.GCP);
                    break;
                case "OPENSTACK":
                    request.setOpenstack(getOpenstackNetworkParameters(Optional.ofNullable(key.getOpenstack()), environmentNetworkResponse));
                    request.setCloudPlatform(CloudPlatform.OPENSTACK);
                    break;
                default:
            }
        }
        return request;
    }

    private InstanceGroupMockNetworkV4Parameters getMockNetworkParameters(Optional<InstanceGroupMockNetworkV1Parameters> mock,
        EnvironmentNetworkResponse value) {
        InstanceGroupMockNetworkV1Parameters params = mock.orElse(new InstanceGroupMockNetworkV1Parameters());
        return convertToMockNetworkParams(new ImmutablePair<>(params, value));
    }

    private InstanceGroupOpenstackNetworkV4Parameters getOpenstackNetworkParameters(Optional<InstanceGroupOpenstackNetworkV1Parameters> openstack,
        EnvironmentNetworkResponse value) {
        InstanceGroupOpenstackNetworkV1Parameters params = openstack.orElse(new InstanceGroupOpenstackNetworkV1Parameters());
        return convertToOpenstackStackRequest(new ImmutablePair<>(params, value));
    }

    private InstanceGroupAzureNetworkV4Parameters getAzureNetworkParameters(Optional<InstanceGroupAzureNetworkV1Parameters> azure,
        EnvironmentNetworkResponse value) {
        InstanceGroupAzureNetworkV1Parameters params = azure.orElse(new InstanceGroupAzureNetworkV1Parameters());
        return convertToAzureStackRequest(new ImmutablePair<>(params, value));
    }

    private InstanceGroupAwsNetworkV4Parameters getAwsNetworkParameters(Optional<InstanceGroupAwsNetworkV1Parameters> key,
        EnvironmentNetworkResponse value) {
        InstanceGroupAwsNetworkV1Parameters params = key.orElse(new InstanceGroupAwsNetworkV1Parameters());
        return convertToAwsStackRequest(new ImmutablePair<>(params, value));
    }

    private InstanceGroupGcpNetworkV4Parameters getGcpNetworkParameters(Optional<InstanceGroupGcpNetworkV1Parameters> key,
        EnvironmentNetworkResponse value) {
        InstanceGroupGcpNetworkV1Parameters params = key.orElse(new InstanceGroupGcpNetworkV1Parameters());
        return convertToGcpStackRequest(new ImmutablePair<>(params, value));
    }

    private InstanceGroupMockNetworkV4Parameters convertToMockNetworkParams(Pair<InstanceGroupMockNetworkV1Parameters,
        EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        InstanceGroupMockNetworkV1Parameters key = source.getKey();

        InstanceGroupMockNetworkV4Parameters params = new InstanceGroupMockNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (value != null) {
                if (subnetIds != null && !subnetIds.isEmpty()) {
                    params.setSubnetIds(subnetIds);
                } else {
                    params.setSubnetIds(List.of(value.getPreferedSubnetId()));
                }
            }
        }

        return params;
    }

    private InstanceGroupAzureNetworkV4Parameters convertToAzureStackRequest(Pair<InstanceGroupAzureNetworkV1Parameters,
        EnvironmentNetworkResponse> source) {
        InstanceGroupAzureNetworkV1Parameters key = source.getKey();

        InstanceGroupAzureNetworkV4Parameters response = new InstanceGroupAzureNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIds != null && !subnetIds.isEmpty()) {
                response.setSubnetIds(subnetIds);
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
        }

        return response;
    }

    private InstanceGroupGcpNetworkV4Parameters convertToGcpStackRequest(Pair<InstanceGroupGcpNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        InstanceGroupGcpNetworkV1Parameters key = source.getKey();

        InstanceGroupGcpNetworkV4Parameters response = new InstanceGroupGcpNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIds != null && !subnetIds.isEmpty()) {
                response.setSubnetIds(subnetIds);
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
        }

        return response;
    }

    private InstanceGroupAwsNetworkV4Parameters convertToAwsStackRequest(Pair<InstanceGroupAwsNetworkV1Parameters,
        EnvironmentNetworkResponse> source) {
        InstanceGroupAwsNetworkV1Parameters key = source.getKey();

        InstanceGroupAwsNetworkV4Parameters response = new InstanceGroupAwsNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIds != null && !subnetIds.isEmpty()) {
                response.setSubnetIds(key.getSubnetIds());
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
        }

        return response;
    }

    private InstanceGroupOpenstackNetworkV4Parameters convertToOpenstackStackRequest(Pair<InstanceGroupOpenstackNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        InstanceGroupOpenstackNetworkV1Parameters key = source.getKey();

        InstanceGroupOpenstackNetworkV4Parameters response = new InstanceGroupOpenstackNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIds != null && !subnetIds.isEmpty()) {
                response.setSubnetIds(key.getSubnetIds());
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
        }

        return response;
    }
}
