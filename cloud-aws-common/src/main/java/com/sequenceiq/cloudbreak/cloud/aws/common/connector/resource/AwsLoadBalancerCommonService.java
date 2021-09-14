package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class AwsLoadBalancerCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLoadBalancerCommonService.class);

    @Inject
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    public List<AwsLoadBalancer> getAwsLoadBalancers(List<CloudLoadBalancer> cloudLoadBalancers, Map<String, List<String>> instanceIdsByGroupName,
            AwsNetworkView awsNetworkView) {
        LOGGER.debug("Converting internal load balancer model to AWS cloud provider model.");
        List<AwsLoadBalancer> awsLoadBalancers = new ArrayList<>();
        for (CloudLoadBalancer cloudLoadBalancer : cloudLoadBalancers) {
            LOGGER.debug("Found load balancer model of type {}", cloudLoadBalancer.getType());
            AwsLoadBalancer loadBalancer = convertLoadBalancer(cloudLoadBalancer, instanceIdsByGroupName, awsNetworkView, awsLoadBalancers);
            if (loadBalancer != null && !awsLoadBalancers.contains(loadBalancer)) {
                awsLoadBalancers.add(loadBalancer);
            }
        }

        Set<String> requestedTypes = cloudLoadBalancers.stream()
                .map(lb -> lb.getType().name())
                .collect(Collectors.toSet());
        Set<String> awsTypes = awsLoadBalancers.stream()
                .map(lb -> AwsLoadBalancerScheme.INTERNAL.awsScheme().equals(lb.getAwsScheme()) ? "PRIVATE" : "PUBLIC")
                .collect(Collectors.toSet());
        if (!requestedTypes.equals(awsTypes)) {
            throw new CloudConnectorException(String.format("Can not create all requested AWS load balancers. " +
                    "Types requested: [%s]; type to be created: [%s]", requestedTypes, awsTypes));
        }

        return awsLoadBalancers;
    }

    @VisibleForTesting
    AwsLoadBalancer convertLoadBalancer(CloudLoadBalancer cloudLoadBalancer, Map<String, List<String>> instanceIdsByGroupName, AwsNetworkView awsNetworkView,
            List<AwsLoadBalancer> awsLoadBalancers) {
        // Check and see if we already have a load balancer whose scheme matches this one.
        AwsLoadBalancer currentLoadBalancer = null;
        LoadBalancerType cloudLbType = cloudLoadBalancer.getType();
        Set<String> subnetIds = selectLoadBalancerSubnetIds(cloudLbType, awsNetworkView, cloudLoadBalancer);
        AwsLoadBalancerScheme scheme = loadBalancerTypeConverter.convert(cloudLbType);

        currentLoadBalancer = awsLoadBalancers.stream()
                .filter(lb -> lb.getScheme() == scheme)
                .findFirst().orElse(new AwsLoadBalancer(scheme));

        currentLoadBalancer.addSubnets(subnetIds);
        setupLoadBalancer(cloudLoadBalancer, instanceIdsByGroupName, currentLoadBalancer);

        return currentLoadBalancer;
    }

    @VisibleForTesting
    Set<String> selectLoadBalancerSubnetIds(LoadBalancerType type, AwsNetworkView awsNetworkView, CloudLoadBalancer cloudLoadBalancer) {
        Set<String> subnetIds = new HashSet<>();
        if (type == LoadBalancerType.PRIVATE) {
            LOGGER.debug("Private load balancer detected. Using instance subnet for load balancer creation.");
            populateSubnetIds(awsNetworkView, cloudLoadBalancer, subnetIds);
        } else {
            LOGGER.debug("Public load balancer detected. Using endpoint gateway subnet for load balancer creation.");
            subnetIds.addAll(awsNetworkView.getEndpointGatewaySubnetList());
            subnetIds.addAll(getEndpointGatewayMultiAZSubnets(cloudLoadBalancer));
            if (subnetIds.isEmpty()) {
                LOGGER.debug("Endpoint gateway subnet is not set. Falling back to instance subnet for load balancer creation.");
                populateSubnetIds(awsNetworkView, cloudLoadBalancer, subnetIds);
            }
        }
        if (subnetIds.isEmpty()) {
            throw new CloudConnectorException("Unable to configure load balancer: Could not identify subnets.");
        }
        return subnetIds;
    }

    private void populateSubnetIds(AwsNetworkView awsNetworkView, CloudLoadBalancer cloudLoadBalancer, Set<String> subnetIds) {
        Set<String> multiAZSubnets = getMultiAZSubnets(cloudLoadBalancer);
        if (multiAZSubnets.isEmpty()) {
            subnetIds.addAll(awsNetworkView.getSubnetList());
        } else {
            subnetIds.addAll(multiAZSubnets);
        }
    }

    private Set<String> getMultiAZSubnets(CloudLoadBalancer cloudLoadBalancer) {
        Set<String> multiAzSubnets = cloudLoadBalancer.getPortToTargetGroupMapping()
                .values()
                .stream()
                .flatMap(groups -> groups.stream().flatMap(group -> group.getNetwork().getSubnets().stream().map(GroupSubnet::getSubnetId)))
                .collect(Collectors.toSet());
        LOGGER.info("Adding subnets that have been configured via multi-AZ support: '{}'", String.join(",", multiAzSubnets));
        return multiAzSubnets;
    }

    private Set<String> getEndpointGatewayMultiAZSubnets(CloudLoadBalancer cloudLoadBalancer) {
        Set<String> multiAzSubnets = cloudLoadBalancer.getPortToTargetGroupMapping()
                .values()
                .stream()
                .flatMap(groups -> groups.stream().flatMap(g -> g.getNetwork().getEndpointGatewaySubnets().stream().map(GroupSubnet::getSubnetId)))
                .collect(Collectors.toSet());
        LOGGER.info("Adding endpoint gateway subnets for multi-AZ support: '{}'", String.join(",", multiAzSubnets));
        return multiAzSubnets;
    }

    private void setupLoadBalancer(CloudLoadBalancer cloudLoadBalancer, Map<String, List<String>> instanceIdsByGroupName,
            AwsLoadBalancer awsLoadBalancer) {
        LOGGER.debug("Configuring target instances for listeners.");
        for (Map.Entry<TargetGroupPortPair, Set<Group>> entry : cloudLoadBalancer.getPortToTargetGroupMapping().entrySet()) {
            AwsListener listener = awsLoadBalancer.getOrCreateListener(entry.getKey().getTrafficPort(), entry.getKey().getHealthCheckPort());
            Set<String> instanceIds = entry.getValue()
                    .stream()
                    .flatMap(tg -> instanceIdsByGroupName.get(tg.getName()).stream())
                    .collect(Collectors.toSet());
            LOGGER.debug(String.format("Adding instances %s to listener %s", instanceIds, listener.getName()));
            listener.addInstancesToTargetGroup(instanceIds);
        }
    }
}
