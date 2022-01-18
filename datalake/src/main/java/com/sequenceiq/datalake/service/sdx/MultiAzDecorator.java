package com.sequenceiq.datalake.service.sdx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Component
public class MultiAzDecorator {

    private static final Set<Tunnel> PUBLIC_SUBNET_SUPPORTED_TUNNEL = Set.of(Tunnel.DIRECT, Tunnel.CLUSTER_PROXY);

    public void decorateStackRequestWithAwsNative(StackV4Request stackV4Request, DetailedEnvironmentResponse environment) {
        if (environment.getCredential().getGovCloud()) {
            stackV4Request.setVariant("AWS_GOV");
        } else {
            stackV4Request.setVariant("AWS_NATIVE");
        }
    }

    public void decorateStackRequestWithMultiAz(StackV4Request stackV4Request, DetailedEnvironmentResponse environment, SdxClusterShape clusterShape) {
        stackV4Request.getInstanceGroups().forEach(ig -> {
            if (ig.getNetwork() == null) {
                ig.setNetwork(new InstanceGroupNetworkV4Request());
                InstanceGroupAwsNetworkV4Parameters networkParameter = ig.getNetwork().createAws();
                List<String> subnetIds;
                if (clusterShape.isMultiAzEnabledByDefault()) {
                    subnetIds = collectMultiAzSubnetIdsForGroup(environment, ig);
                } else {
                    subnetIds = List.of(environment.getNetwork().getPreferedSubnetId());
                }
                networkParameter.setSubnetIds(subnetIds);
            }
        });
    }

    private List<String> collectMultiAzSubnetIdsForGroup(DetailedEnvironmentResponse environment, InstanceGroupV4Request ig) {
        List<String> subnetIds;
        if (ig.getType() == InstanceGroupType.GATEWAY) {
            subnetIds = getSubnetsForGateway(environment);
        } else {
            subnetIds = new ArrayList<>(environment.getNetwork().getSubnetIds());
        }
        return subnetIds;
    }

    private List<String> getSubnetsForGateway(DetailedEnvironmentResponse environment) {
        List<String> subnetIds = new ArrayList<>();
        if (PUBLIC_SUBNET_SUPPORTED_TUNNEL.contains(environment.getTunnel())) {
            subnetIds = distinctSubnetByAz(environment.getNetwork(), SubnetType.PUBLIC);
        }
        if (subnetIds.isEmpty()) {
            subnetIds = distinctSubnetByAz(environment.getNetwork(), SubnetType.PRIVATE);
        }
        return subnetIds;
    }

    private List<String> distinctSubnetByAz(EnvironmentNetworkResponse network, SubnetType subnetType) {
        Map<String, String> distinctedSubnetByAz = new HashMap<>();
        network.getSubnetMetas().forEach((subnet, cloudSubnet) -> {
            if (cloudSubnet.getType() == subnetType && !distinctedSubnetByAz.containsKey(cloudSubnet.getAvailabilityZone())) {
                distinctedSubnetByAz.put(cloudSubnet.getAvailabilityZone(), subnet);
            }
        });
        return new ArrayList<>(distinctedSubnetByAz.values());
    }
}
