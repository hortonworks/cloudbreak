package com.sequenceiq.cloudbreak.cloud.aws.validator;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class AwsGatewaySubnetMultiAzValidator implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsGatewaySubnetMultiAzValidator.class);

    @Inject
    private CommonAwsClient awsClient;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        LOGGER.debug("Validating that only one subnet per availability zone have been provided for the gateway group.");
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        Optional<Group> gatewayGroupOptional = getGatewayGroup(cloudStack);
        if (gatewayGroupOptional.isPresent()) {
            Group group = gatewayGroupOptional.get();
            Set<String> subnetIds = getInstanceGroupNetworSubnetIds(group);
            if (!subnetIds.isEmpty()) {
                Map<String, Set<String>> subnetIdSetsByAvailabilityZone = fetchingSubnetsAndGroupingByAvailabilityZone(awsCredential, subnetIds, region);
                Set<String> availabilityZonesWithMultipleSubnets = subnetIdSetsByAvailabilityZone.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().size() > 1)
                        .map(e -> String.format("subnets: '%s' from availability zone: '%s'", String.join(",", e.getValue()), e.getKey()))
                        .collect(toSet());

                if (!availabilityZonesWithMultipleSubnets.isEmpty()) {
                    LOGGER.debug("Validation error found there are multiple subnets associated with an availability zone");
                    String validationError = String.format("Only one subnet per Availability Zone can be specified for a Gateway group. The request contains %s",
                            String.join(", ", availabilityZonesWithMultipleSubnets));
                    LOGGER.info(validationError);
                    throw new CloudConnectorException(validationError);
                }
            } else {
                LOGGER.debug("No group network subnet specified, there is no info to be validated");
            }
            LOGGER.debug("There is no validation error found for gateway's group network's subnets");
        }
    }

    private Optional<Group> getGatewayGroup(CloudStack cloudStack) {
        return cloudStack.getGroups()
                .stream()
                .filter(gr -> InstanceGroupType.GATEWAY.equals(gr.getType()))
                .findFirst();
    }

    private Set<String> getInstanceGroupNetworSubnetIds(Group group) {
        Set<String> result = new HashSet<>();
        GroupNetwork network = group.getNetwork();
        if (network != null) {
            result = Optional.ofNullable(network.getSubnets())
                    .orElse(new HashSet<>())
                    .stream()
                    .map(GroupSubnet::getSubnetId)
                    .collect(Collectors.toSet());
        }
        return result;
    }

    private Map<String, Set<String>> fetchingSubnetsAndGroupingByAvailabilityZone(AwsCredentialView awsCredential, Set<String> subnetIds, String region) {
        String subnetIdsJoined = String.join(",", subnetIds);
        LOGGER.info("Fetching subnets('{}') for validating availability zones for gateway group", subnetIdsJoined);
        DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest()
                .withSubnetIds(subnetIds);
        try {
            AmazonEc2Client ec2Client = awsClient.createEc2Client(awsCredential, region);
            DescribeSubnetsResult describeSubnetsResult = ec2Client.describeSubnets(describeSubnetsRequest);
            return describeSubnetsResult.getSubnets()
                    .stream()
                    .collect(groupingBy(Subnet::getAvailabilityZone, mapping(Subnet::getSubnetId, toSet())));
        } catch (AmazonServiceException ex) {
            String msg = String.format("Failed to fetch subnets with id: '%s' during the validation of subnets per availability zone for gateway group.",
                    subnetIdsJoined);
            LOGGER.warn(msg, ex);
            throw new CloudConnectorException(msg, ex);
        }
    }
}
