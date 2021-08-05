package com.sequenceiq.environment.environment.flow.creation.handler.freeipa.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaNetworkProvider;
import com.sequenceiq.environment.network.service.domain.ProvidedSubnetIds;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.SubnetIdProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupNetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.InstanceGroupAwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAwsNetworkProvider implements FreeIpaNetworkProvider {

    @Inject
    private SubnetIdProvider subnetIdProvider;

    @Override
    public NetworkRequest network(EnvironmentDto environment, boolean multiAzRequired) {
        NetworkDto network = environment.getNetwork();
        NetworkRequest networkRequest = new NetworkRequest();
        AwsParams awsParams = network.getAws();
        AwsNetworkParameters awsNetworkParameters = new AwsNetworkParameters();
        networkRequest.setNetworkCidrs(collectNetworkCidrs(network));
        networkRequest.setOutboundInternetTraffic(network.getOutboundInternetTraffic());
        awsNetworkParameters.setVpcId(awsParams.getVpcId());
        if (!multiAzRequired) {
            ProvidedSubnetIds providedSubnetIds = subnetIdProvider.subnets(
                    environment.getNetwork(),
                    environment.getExperimentalFeatures().getTunnel(),
                    CloudPlatform.AWS,
                    multiAzRequired);
            awsNetworkParameters.setSubnetId(providedSubnetIds.getSubnetId());
        }
        networkRequest.setAws(awsNetworkParameters);
        return networkRequest;
    }

    @Override
    public InstanceGroupNetworkRequest networkByGroup(EnvironmentDto environment) {
        InstanceGroupNetworkRequest instanceGroupNetworkRequest = new InstanceGroupNetworkRequest();
        ProvidedSubnetIds providedSubnetIds = subnetIdProvider.subnets(
                environment.getNetwork(),
                environment.getExperimentalFeatures().getTunnel(),
                CloudPlatform.AWS,
                true);
        InstanceGroupAwsNetworkParameters instanceGroupAwsNetworkParameters = new InstanceGroupAwsNetworkParameters();
        instanceGroupAwsNetworkParameters.setSubnetIds(providedSubnetIds.getSubnetIds()
                .stream()
                .collect(Collectors.toList()));
        instanceGroupNetworkRequest.setAws(instanceGroupAwsNetworkParameters);
        return instanceGroupNetworkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        AwsNetworkParameters awsNetwork = networkRequest.getAws();
        if (awsNetwork.getSubnetId() != null) {
            return environment.getNetwork().getSubnetMetas().get(awsNetwork.getSubnetId()).getAvailabilityZone();
        }
        return null;
    }

    @Override
    public String availabilityZone(InstanceGroupNetworkRequest networkRequest, EnvironmentDto environment) {
        InstanceGroupAwsNetworkParameters awsNetwork = networkRequest.getAws();
        if (awsNetwork.getSubnetIds() != null && !awsNetwork.getSubnetIds().isEmpty()) {
            return environment.getNetwork().getSubnetMetas().get(awsNetwork.getSubnetIds().get(0)).getAvailabilityZone();
        }
        return null;
    }

    @Override
    public Set<String> subnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getAws().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    private List<String> collectNetworkCidrs(NetworkDto network) {
        return CollectionUtils.isNotEmpty(network.getNetworkCidrs()) ? new ArrayList<>(network.getNetworkCidrs()) : List.of();
    }
}
