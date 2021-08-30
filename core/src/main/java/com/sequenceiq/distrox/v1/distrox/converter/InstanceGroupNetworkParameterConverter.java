package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.azure.InstanceGroupAzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.gcp.InstanceGroupGcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.mock.InstanceGroupMockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.openstack.InstanceGroupOpenstackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.yarn.InstanceGroupYarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.InstanceGroupAzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.InstanceGroupGcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.InstanceGroupMockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.openstack.InstanceGroupOpenstackNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.yarn.InstanceGroupYarnNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class InstanceGroupNetworkParameterConverter {

    public InstanceGroupMockNetworkV4Parameters convert(InstanceGroupMockNetworkV1Parameters mock,
        EnvironmentNetworkResponse value, CloudPlatform cloudPlatform) {
        InstanceGroupMockNetworkV4Parameters response = null;
        if (CloudPlatform.MOCK == cloudPlatform) {
            InstanceGroupMockNetworkV1Parameters params = mock == null
                    ? new InstanceGroupMockNetworkV1Parameters() : mock;
            response = convertToMockNetworkParams(new ImmutablePair<>(params, value));
        }
        return response;
    }

    public InstanceGroupOpenstackNetworkV4Parameters convert(InstanceGroupOpenstackNetworkV1Parameters openstack,
        EnvironmentNetworkResponse value, CloudPlatform cloudPlatform) {
        InstanceGroupOpenstackNetworkV4Parameters response = null;
        if (CloudPlatform.OPENSTACK == cloudPlatform) {
            InstanceGroupOpenstackNetworkV1Parameters params = openstack == null
                    ? new InstanceGroupOpenstackNetworkV1Parameters() : openstack;
            response = convertToOpenstackNetworkParams(new ImmutablePair<>(params, value));
        }
        return response;
    }

    public InstanceGroupAzureNetworkV4Parameters convert(InstanceGroupAzureNetworkV1Parameters azure,
        EnvironmentNetworkResponse value, CloudPlatform cloudPlatform) {
        InstanceGroupAzureNetworkV4Parameters response = null;
        if (CloudPlatform.AZURE == cloudPlatform) {
            InstanceGroupAzureNetworkV1Parameters params = azure == null
                    ? new InstanceGroupAzureNetworkV1Parameters() : azure;
            response = convertToAzureNetworkParams(new ImmutablePair<>(params, value));
        }
        return response;
    }

    public InstanceGroupAwsNetworkV4Parameters convert(InstanceGroupAwsNetworkV1Parameters aws,
        EnvironmentNetworkResponse value, CloudPlatform cloudPlatform) {
        InstanceGroupAwsNetworkV4Parameters response = null;
        if (CloudPlatform.AWS == cloudPlatform) {
            InstanceGroupAwsNetworkV1Parameters params = aws == null ?
                    new InstanceGroupAwsNetworkV1Parameters() : aws;
            response = convertToAwsNetworkParams(new ImmutablePair<>(params, value));
        }
        return response;
    }

    public InstanceGroupGcpNetworkV4Parameters convert(InstanceGroupGcpNetworkV1Parameters gcp,
        EnvironmentNetworkResponse value, CloudPlatform cloudPlatform) {
        InstanceGroupGcpNetworkV4Parameters response = null;
        if (CloudPlatform.GCP == cloudPlatform) {
            InstanceGroupGcpNetworkV1Parameters params = gcp == null
                    ? new InstanceGroupGcpNetworkV1Parameters() : gcp;
            response = convertToGcpNetworkParams(new ImmutablePair<>(params, value));
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

    private InstanceGroupMockNetworkV4Parameters convertToMockNetworkParams(Pair<InstanceGroupMockNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        EnvironmentNetworkResponse value = source.getValue();
        InstanceGroupMockNetworkV1Parameters key = source.getKey();

        InstanceGroupMockNetworkV4Parameters params = new InstanceGroupMockNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (value != null) {
                if (subnetIdsDefined(subnetIds)) {
                    params.setSubnetIds(subnetIds);
                } else {
                    params.setSubnetIds(List.of(value.getPreferedSubnetId()));
                }
            }
        }

        return params;
    }

    private InstanceGroupAzureNetworkV4Parameters convertToAzureNetworkParams(Pair<InstanceGroupAzureNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        InstanceGroupAzureNetworkV1Parameters key = source.getKey();

        InstanceGroupAzureNetworkV4Parameters response = new InstanceGroupAzureNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIdsDefined(subnetIds)) {
                response.setSubnetIds(subnetIds);
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
        }

        return response;
    }

    private InstanceGroupGcpNetworkV4Parameters convertToGcpNetworkParams(Pair<InstanceGroupGcpNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        InstanceGroupGcpNetworkV1Parameters key = source.getKey();

        InstanceGroupGcpNetworkV4Parameters response = new InstanceGroupGcpNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIdsDefined(subnetIds)) {
                response.setSubnetIds(subnetIds);
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
        }

        return response;
    }

    private InstanceGroupYarnNetworkV4Parameters convertToYarnNetworkParams(Pair<InstanceGroupYarnNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        return new InstanceGroupYarnNetworkV4Parameters();
    }

    private InstanceGroupAwsNetworkV4Parameters convertToAwsNetworkParams(Pair<InstanceGroupAwsNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        InstanceGroupAwsNetworkV1Parameters key = source.getKey();

        InstanceGroupAwsNetworkV4Parameters response = new InstanceGroupAwsNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIdsDefined(subnetIds)) {
                response.setSubnetIds(subnetIds);
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
            List<String> endpointGatewaySubnetIds = key.getEndpointGatewaySubnetIds();
            if (subnetIdsDefined(endpointGatewaySubnetIds)) {
                response.setEndpointGatewaySubnetIds(endpointGatewaySubnetIds);
            } else if (source.getValue() != null) {
                response.setEndpointGatewaySubnetIds(List.copyOf(source.getValue().getEndpointGatewaySubnetIds()));
            }
        }

        return response;
    }

    private InstanceGroupOpenstackNetworkV4Parameters convertToOpenstackNetworkParams(Pair<InstanceGroupOpenstackNetworkV1Parameters,
            EnvironmentNetworkResponse> source) {
        InstanceGroupOpenstackNetworkV1Parameters key = source.getKey();

        InstanceGroupOpenstackNetworkV4Parameters response = new InstanceGroupOpenstackNetworkV4Parameters();

        if (key != null) {
            List<String> subnetIds = key.getSubnetIds();
            if (subnetIdsDefined(subnetIds)) {
                response.setSubnetIds(subnetIds);
            } else if (source.getValue() != null) {
                response.setSubnetIds(List.of(source.getValue().getPreferedSubnetId()));
            }
        }

        return response;
    }

    private boolean subnetIdsDefined(List<String> subnetIds) {
        return subnetIds != null && !subnetIds.isEmpty();
    }

}
