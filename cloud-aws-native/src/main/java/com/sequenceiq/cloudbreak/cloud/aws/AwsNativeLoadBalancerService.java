package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AvailabilityZone;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancerAttributesResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SetSubnetsRequest;

@Service
public class AwsNativeLoadBalancerService {

    public static final String CROSS_ZONE_LOAD_BALANCING_ENABLED = "load_balancing.cross_zone.enabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeLoadBalancerService.class);

    private static final int SUBNET_UPDATE_POLLING_TIMEOUT_SECONDS = 300;

    private static final int SUBNET_UPDATE_POLLING_INTERVAL_SECONDS = 5;

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Retryable(retryFor = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public List<String> getLoadBalancerIps(AmazonEc2Client ec2Client, String loadBalancerName) {
        try {
            List<String> ips = describeLoadBalancerEnis(ec2Client, loadBalancerName).stream()
                    .map(NetworkInterface::privateIpAddress)
                    .filter(Objects::nonNull)
                    .toList();
            if (ips.isEmpty()) {
                String errorMessage = String.format("Failed to get the IP address for load balancer: %s", loadBalancerName);
                LOGGER.debug(errorMessage);
                throw new NotFoundException(errorMessage);
            }
            return ips;
        } catch (Ec2Exception e) {
            LOGGER.error("Failed to retrieve AWS network interfaces for load balancer {}", loadBalancerName, e);
            throw new CloudConnectorException(e);
        }
    }

    public void updateMultiAzLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack) {
        LOGGER.info("Stack is multi-AZ, updating load balancer subnets and enabling cross-zone load balancing");
        try {
            AwsCredentialView awsCredentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
            String region = authenticatedContext.getCloudContext().getLocation().getRegion().value();
            AmazonElasticLoadBalancingClient elasticLoadBalancingClient = awsClient.createElasticLoadBalancingClient(awsCredentialView, region);
            AmazonEc2Client ec2Client = awsClient.createEc2Client(awsCredentialView, region);

            Set<String> environmentSubnets = getMultiAzSubnetsForLoadBalancer(stack);
            List<CloudResource> loadbalancerResources = resourceRetriever.findAllByStatusAndTypeAndStack(
                    CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, authenticatedContext.getCloudContext().getId());
            for (CloudResource loadBalancer : loadbalancerResources) {
                String loadBalancerArn = loadBalancer.getReference();
                LOGGER.info("Updating load balancer {}", loadBalancerArn);

                if (!isCrossZoneLoadBalancingEnabled(elasticLoadBalancingClient, loadBalancerArn)) {
                    LOGGER.info("Enabling cross-zone load balancing for {}", loadBalancerArn);
                    modifyLoadBalancerAttributes(elasticLoadBalancingClient, loadBalancerArn,
                            Map.of(CROSS_ZONE_LOAD_BALANCING_ENABLED, Boolean.TRUE.toString()));
                }

                if (!environmentSubnets.isEmpty()) {
                    LoadBalancer describedLoadBalancer = describeLoadBalancer(elasticLoadBalancingClient, loadBalancerArn);
                    Set<String> currentSubnets = extractSubnetIds(describedLoadBalancer);
                    Set<String> targetSubnets = resolveTargetSubnets(ec2Client, describedLoadBalancer, environmentSubnets);

                    if (!targetSubnets.equals(currentSubnets)) {
                        LOGGER.info("Load balancer {} subnets changing from {} to {}",
                                loadBalancerArn, String.join(",", currentSubnets), String.join(",", targetSubnets));
                        setLoadBalancerSubnets(elasticLoadBalancingClient, loadBalancerArn, targetSubnets);
                    } else {
                        LOGGER.info("Load balancer {} subnets already up to date", loadBalancerArn);
                    }
                }
            }
        } catch (SdkException e) {
            LOGGER.error("Failed to update load balancer for multi-AZ stack", e);
            throw new CloudConnectorException("Failed to update load balancer: " + e.getMessage(), e);
        }
    }

    public void waitForLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String region = authenticatedContext.getCloudContext().getLocation().getRegion().value();
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = awsClient.createElasticLoadBalancingClient(awsCredentialView, region);
        AmazonEc2Client ec2Client = awsClient.createEc2Client(awsCredentialView, region);
        long stackId = authenticatedContext.getCloudContext().getId();

        Set<String> environmentSubnets = getMultiAzSubnetsForLoadBalancer(stack);
        if (environmentSubnets.isEmpty()) {
            LOGGER.info("No multi-AZ subnets configured on stack {}, skipping wait for load balancer network interfaces", stackId);
        } else {
            List<CloudResource> loadbalancerResources =
                    resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, stackId);
            for (CloudResource loadBalancer : loadbalancerResources) {
                String loadBalancerArn = loadBalancer.getReference();
                LoadBalancer describedLoadBalancer = describeLoadBalancer(elasticLoadBalancingClient, loadBalancerArn);
                Set<String> expectedSubnets = resolveTargetSubnets(ec2Client, describedLoadBalancer, environmentSubnets);

                LOGGER.info("Waiting for load balancer {} network interfaces to be provisioned on subnets {}", loadBalancerArn, expectedSubnets);
                try {
                    waitForLoadBalancerEnisOnSubnets(ec2Client, describedLoadBalancer.loadBalancerName(), expectedSubnets);
                } catch (RuntimeException e) {
                    throw new CloudConnectorException(String.format(
                            "Failed while waiting for load balancer %s network interfaces to be provisioned on subnets %s: %s",
                            describedLoadBalancer.loadBalancerName(), expectedSubnets, e.getMessage()), e);
                }
            }
        }
    }

    private boolean isCrossZoneLoadBalancingEnabled(AmazonElasticLoadBalancingClient client, String loadBalancerArn) {
        try {
            DescribeLoadBalancerAttributesResponse response = client.describeLoadBalancerAttributes(
                    DescribeLoadBalancerAttributesRequest.builder()
                            .loadBalancerArn(loadBalancerArn)
                            .build());
            return response.attributes().stream()
                    .filter(attr -> CROSS_ZONE_LOAD_BALANCING_ENABLED.equals(attr.key()))
                    .map(LoadBalancerAttribute::value)
                    .map(Boolean::parseBoolean)
                    .findFirst()
                    .orElse(false);
        } catch (Exception e) {
            LOGGER.error("Failed to describe load balancer attributes for {}", loadBalancerArn, e);
            throw e;
        }
    }

    private LoadBalancer describeLoadBalancer(AmazonElasticLoadBalancingClient client, String loadBalancerArn) {
        try {
            DescribeLoadBalancersResponse response = client.describeLoadBalancers(
                    DescribeLoadBalancersRequest.builder()
                            .loadBalancerArns(loadBalancerArn)
                            .build());
            return response.loadBalancers().stream()
                    .findFirst()
                    .orElseThrow(() -> new CloudConnectorException("Load balancer not found: " + loadBalancerArn));
        } catch (Exception e) {
            LOGGER.error("Failed to describe load balancer {}", loadBalancerArn, e);
            throw e;
        }
    }

    private Set<String> extractSubnetIds(LoadBalancer loadBalancer) {
        return loadBalancer.availabilityZones().stream()
                .map(AvailabilityZone::subnetId)
                .collect(Collectors.toSet());
    }

    /**
     * Collects the subnets the load balancer should span: the ones it already has, plus one candidate subnet for every availability zone it does not
     * serve yet. A load balancer can have at most one subnet per availability zone, so candidates in an already served zone are dropped, and where
     * several candidates share a zone only one of them is kept.
     */
    private Set<String> resolveTargetSubnets(AmazonEc2Client ec2Client, LoadBalancer loadBalancer, Set<String> candidateSubnetIds) {
        Set<String> targetSubnets = new HashSet<>();
        Set<String> servedZones = new HashSet<>();
        for (AvailabilityZone availabilityZone : loadBalancer.availabilityZones()) {
            targetSubnets.add(availabilityZone.subnetId());
            servedZones.add(availabilityZone.zoneName());
        }

        Set<String> newCandidates = candidateSubnetIds.stream()
                .filter(subnetId -> !targetSubnets.contains(subnetId))
                .collect(Collectors.toSet());
        Map<String, List<String>> candidatesByZone = getCandidateSubnetIdsByZone(ec2Client, newCandidates);
        Set<String> resolvedSubnetIds = candidatesByZone.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        if (resolvedSubnetIds.size() < newCandidates.size()) {
            LOGGER.warn("Could not determine the availability zone of subnets {}, they are not added to load balancer {}",
                    newCandidates.stream().filter(subnetId -> !resolvedSubnetIds.contains(subnetId)).collect(Collectors.toSet()),
                    loadBalancer.loadBalancerArn());
        }

        Set<String> skippedZones = candidatesByZone.keySet().stream()
                .filter(servedZones::contains)
                .collect(Collectors.toSet());
        if (!skippedZones.isEmpty()) {
            LOGGER.debug("Availability zones {} are already served by load balancer {}, skipping their candidate subnets",
                    skippedZones, loadBalancer.loadBalancerArn());
        }

        candidatesByZone.entrySet().stream()
                .filter(zoneCandidates -> !servedZones.contains(zoneCandidates.getKey()))
                .map(zoneCandidates -> Collections.min(zoneCandidates.getValue()))
                .forEach(targetSubnets::add);
        return targetSubnets;
    }

    private Map<String, List<String>> getCandidateSubnetIdsByZone(AmazonEc2Client ec2Client, Set<String> subnetIds) {
        if (subnetIds.isEmpty()) {
            return Map.of();
        }
        return ec2Client.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(subnetIds).build()).subnets().stream()
                .collect(Collectors.groupingBy(Subnet::availabilityZone, Collectors.mapping(Subnet::subnetId, Collectors.toList())));
    }

    private void waitForLoadBalancerEnisOnSubnets(AmazonEc2Client ec2Client, String loadBalancerName, Set<String> expectedSubnets) {
        Polling.stopAfterDelay(SUBNET_UPDATE_POLLING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .waitPeriodly(SUBNET_UPDATE_POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .stopIfException(true)
                .run(() -> {
                    try {
                        Set<String> eniSubnets = describeLoadBalancerEniSubnets(ec2Client, loadBalancerName);
                        if (eniSubnets.containsAll(expectedSubnets)) {
                            LOGGER.info("Load balancer {} network interfaces are provisioned on all expected subnets: {}", loadBalancerName, eniSubnets);
                            return AttemptResults.justFinish();
                        } else {
                            LOGGER.debug("Load balancer {} network interfaces on subnets {}, waiting for {}", loadBalancerName, eniSubnets, expectedSubnets);
                            return AttemptResults.justContinue();
                        }
                    } catch (Ec2Exception e) {
                        return e.isRetryableException() ? AttemptResults.justContinue() : AttemptResults.breakFor(e);
                    }
                });
    }

    private Set<String> describeLoadBalancerEniSubnets(AmazonEc2Client ec2Client, String loadBalancerName) {
        return describeLoadBalancerEnis(ec2Client, loadBalancerName).stream()
                .map(NetworkInterface::subnetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<NetworkInterface> describeLoadBalancerEnis(AmazonEc2Client ec2Client, String loadBalancerName) {
        DescribeNetworkInterfacesRequest request = DescribeNetworkInterfacesRequest.builder()
                .filters(Filter.builder()
                        .name("description")
                        .values("ELB net/" + loadBalancerName + "/*")
                        .build())
                .build();
        return ec2Client.describeNetworkInterfaces(request).networkInterfaces();
    }

    private void setLoadBalancerSubnets(AmazonElasticLoadBalancingClient client, String loadBalancerArn, Set<String> subnetIds) {
        LOGGER.info("Setting subnets for load balancer {}: {}", loadBalancerArn, String.join(",", subnetIds));
        try {
            SetSubnetsRequest request = SetSubnetsRequest.builder()
                    .loadBalancerArn(loadBalancerArn)
                    .subnets(subnetIds)
                    .build();
            client.setSubnets(request);
            LOGGER.info("Subnets successfully set for load balancer {}", loadBalancerArn);
        } catch (Exception e) {
            LOGGER.error("Failed to set subnets for load balancer {}", loadBalancerArn, e);
            throw e;
        }
    }

    private void modifyLoadBalancerAttributes(AmazonElasticLoadBalancingClient client, String loadBalancerArn, Map<String, String> attributes) {
        LOGGER.info("Modifying attributes for load balancer '{}' to {}", loadBalancerArn, attributes);
        try {
            ModifyLoadBalancerAttributesRequest request = ModifyLoadBalancerAttributesRequest.builder()
                    .loadBalancerArn(loadBalancerArn)
                    .attributes(attributes.entrySet().stream()
                            .map(e -> LoadBalancerAttribute.builder()
                                    .key(e.getKey())
                                    .value(e.getValue())
                                    .build())
                            .toList())
                    .build();
            client.modifyLoadBalancerAttributes(request);
            LOGGER.info("Modified attributes for load balancer '{}'", loadBalancerArn);
        } catch (Exception e) {
            LOGGER.error("Failed to modify attributes for load balancer '{}'", loadBalancerArn, e);
            throw e;
        }
    }

    private Set<String> getMultiAzSubnetsForLoadBalancer(CloudStack stack) {
        return Optional.ofNullable(stack.getGroups()).orElse(List.of()).stream()
                .map(Group::getNetwork)
                .filter(Objects::nonNull)
                .map(GroupNetwork::getSubnets)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .map(GroupSubnet::getSubnetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
