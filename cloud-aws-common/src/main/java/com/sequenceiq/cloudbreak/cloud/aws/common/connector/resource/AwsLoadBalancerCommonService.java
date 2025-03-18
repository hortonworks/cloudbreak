package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.ACCESS_DENIED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsResourceException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ElasticLoadBalancingV2Exception;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyTargetGroupAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupNotFoundException;

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
                .map(lb -> lb.getScheme().getLoadBalancerType().name())
                .collect(Collectors.toSet());
        if (!requestedTypes.equals(awsTypes)) {
            throw new CloudConnectorException(String.format("Can not create all requested AWS load balancers. " +
                    "Types requested: [%s]; type to be created: [%s]", requestedTypes, awsTypes));
        }

        return awsLoadBalancers;
    }

    public void disableDeletionProtection(AmazonElasticLoadBalancingClient client, String arn) {
        disableDeletionProtection(client, List.of(arn));
    }

    public void disableDeletionProtection(AmazonElasticLoadBalancingClient client, List<String> arns) {
        arns.forEach(arn -> modifyLoadBalancerAttributes(client, arn, false));
    }

    public void modifyLoadBalancerAttributes(AmazonElasticLoadBalancingClient client, String arn) {
        modifyLoadBalancerAttributes(client, List.of(arn));
    }

    public void modifyLoadBalancerAttributes(AmazonElasticLoadBalancingClient client, List<String> arns) {
        arns.forEach(arn -> modifyLoadBalancerAttributes(client, arn, true));
    }

    public void modifyTargetGroupAttributes(AmazonElasticLoadBalancingClient client, String arn, boolean stickySession) {
        try {
            DescribeTargetGroupsResponse describeTargetGroupsResponse =
                    client.describeTargetGroup(DescribeTargetGroupsRequest.builder().loadBalancerArn(arn).build());
            for (TargetGroup targetGroup : describeTargetGroupsResponse.targetGroups()) {
                if (stickySession) {
                    LOGGER.debug("Setting sticky session and cross-zone enablement attribute for target group '{}'", targetGroup.targetGroupArn());
                    Set<TargetGroupAttribute> targetGroupAttributes = new HashSet<>(Set.of(
                            TargetGroupAttribute.builder()
                                    .key("stickiness.enabled")
                                    .value("true")
                                    .build()));
                    ModifyTargetGroupAttributesRequest modifyRequest = ModifyTargetGroupAttributesRequest.builder()
                            .targetGroupArn(targetGroup.targetGroupArn())
                            .attributes(targetGroupAttributes)
                            .build();
                    LOGGER.debug("Modifying target group ({}) attributes with: {}", targetGroup.targetGroupArn(), targetGroupAttributes);
                    client.modifyTargetGroupAttributes(modifyRequest);
                }
            }
        } catch (LoadBalancerNotFoundException loadBalancerNotFoundException) {
            LOGGER.info("Load balancer has already been deleted with ARN '{}', no need to modify attribute.", arn);
        } catch (TargetGroupNotFoundException targetGroupNotFoundException) {
            LOGGER.info("TargetGroup has already been deleted with ARN '{}', no need to modify attribute.", arn);
        } catch (AwsServiceException amazonServiceException) {
            LOGGER.warn("Failed to modify targetgroup flag of load balancer to value '{}'", stickySession, amazonServiceException);
            if (amazonServiceException instanceof ElasticLoadBalancingV2Exception
                    && ACCESS_DENIED.equals(amazonServiceException.awsErrorDetails().errorCode())) {
                LOGGER.info("User has no right to edit loadbalancer targetgroup to be stickysession. " +
                        "The role needs elasticloadbalancing:ModifyLoadBalancerAttributes permission");
            } else {
                throw amazonServiceException;
            }
        }
    }

    private void modifyLoadBalancerAttributes(AmazonElasticLoadBalancingClient client, String arn, boolean deletionProtection) {
        try {
            Set<LoadBalancerAttribute> loadBalancerAttributes = new HashSet<>();
            loadBalancerAttributes.add(LoadBalancerAttribute.builder()
                    .key("deletion_protection.enabled")
                    .value(String.valueOf(deletionProtection))
                    .build());
            loadBalancerAttributes.add(LoadBalancerAttribute.builder()
                    .key("load_balancing.cross_zone.enabled")
                    .value("true")
                    .build());

            ModifyLoadBalancerAttributesRequest modifyLoadBalancerAttributesRequest = ModifyLoadBalancerAttributesRequest.builder()
                    .loadBalancerArn(arn)
                    .attributes(loadBalancerAttributes)
                    .build();
            client.modifyLoadBalancerAttributes(modifyLoadBalancerAttributesRequest);
        } catch (LoadBalancerNotFoundException loadBalancerNotFoundException) {
            if (!deletionProtection) {
                LOGGER.info("Load balancer has already been deleted with ARN '{}', no need to remove deletion protection.", arn);
            } else {
                String errorMessage = String.format("No load balancer found with ARN '%s' to enable deletion protection", arn);
                LOGGER.warn(errorMessage, loadBalancerNotFoundException);
                throw new AwsResourceException(errorMessage, loadBalancerNotFoundException);
            }
        } catch (AwsServiceException amazonServiceException) {
            LOGGER.warn("Failed to modify deletion protection flag of load balancer to value '{}'", deletionProtection, amazonServiceException);
            if (amazonServiceException instanceof ElasticLoadBalancingV2Exception
                    && ACCESS_DENIED.equals(amazonServiceException.awsErrorDetails().errorCode())) {
                LOGGER.info("User has no right to edit loadbalancer to be deletionprotected. " +
                        "The role needs elasticloadbalancing:ModifyLoadBalancerAttributes permission");
            } else {
                throw amazonServiceException;
            }
        }
    }

    @VisibleForTesting
    AwsLoadBalancer convertLoadBalancer(CloudLoadBalancer cloudLoadBalancer, Map<String, List<String>> instanceIdsByGroupName, AwsNetworkView awsNetworkView,
            List<AwsLoadBalancer> awsLoadBalancers) {
        // Check and see if we already have a load balancer whose scheme matches this one.
        AwsLoadBalancer currentLoadBalancer;
        LoadBalancerType cloudLbType = cloudLoadBalancer.getType();
        Set<String> subnetIds = selectLoadBalancerSubnetIds(cloudLbType, awsNetworkView, cloudLoadBalancer);
        AwsLoadBalancerScheme scheme = loadBalancerTypeConverter.convert(cloudLbType);

        currentLoadBalancer = awsLoadBalancers.stream()
                .filter(lb -> lb.getScheme() == scheme)
                .findFirst().orElse(new AwsLoadBalancer(scheme));

        currentLoadBalancer.addSubnets(subnetIds);
        currentLoadBalancer.setUseStickySessionForTargetGroup(cloudLoadBalancer.isStickySession());
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
            LOGGER.debug("{} load balancer detected. Using endpoint gateway subnet for load balancer creation.", type);
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

    private void setupLoadBalancer(CloudLoadBalancer cloudLoadBalancer, Map<String, List<String>> instanceIdsByGroupName, AwsLoadBalancer awsLoadBalancer) {
        LOGGER.debug("Configuring target instances for listeners.");
        for (Map.Entry<TargetGroupPortPair, Set<Group>> entry : cloudLoadBalancer.getPortToTargetGroupMapping().entrySet()) {
            TargetGroupPortPair targetGroup = entry.getKey();
            ProtocolEnum protocol = Optional.ofNullable(targetGroup.getTrafficProtocol()).map(p -> ProtocolEnum.fromValue(p.name())).orElse(null);
            HealthProbeParameters healthProbe = targetGroup.getHealthProbeParameters();
            AwsListener listener = awsLoadBalancer.getOrCreateListener(
                    targetGroup.getTrafficPort(),
                    protocol,
                    healthProbe);
            Set<String> instanceIds = entry.getValue()
                    .stream()
                    .flatMap(tg -> instanceIdsByGroupName.get(tg.getName()).stream())
                    .collect(Collectors.toSet());
            LOGGER.debug(String.format("Adding instances %s to listener %s", instanceIds, listener.getName()));
            listener.addInstancesToTargetGroup(instanceIds);
        }
    }
}
