package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;

import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.Instance;

public class AsgInstanceDetachWaiter implements AttemptMaker<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsgInstanceDetachWaiter.class);

    private final AmazonAutoScalingClient amazonASClient;

    private final String asGroupName;

    private final List<String> instanceIdsToDetach;

    public AsgInstanceDetachWaiter(AmazonAutoScalingClient amazonASClient, String asGroupName, List<String> instanceIdsToDetach) {
        this.amazonASClient = amazonASClient;
        this.asGroupName = asGroupName;
        this.instanceIdsToDetach = instanceIdsToDetach;
    }

    @Override
    public AttemptResult<Boolean> process() throws Exception {
        DescribeAutoScalingGroupsResponse result = amazonASClient.describeAutoScalingGroups(
                DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(asGroupName).build());
        Set<String> instanceIdsInASG = result.autoScalingGroups().stream()
                .flatMap(autoScalingGroup -> autoScalingGroup.instances().stream().map(Instance::instanceId))
                .collect(Collectors.toSet());
        Set<String> remainingInstances = instanceIdsToDetach.stream().filter(instanceIdsInASG::contains).collect(Collectors.toSet());
        if (remainingInstances.size() > 0) {
            LOGGER.info("Remaining instances in detaching process for group '{}': {}", asGroupName, remainingInstances);
            return AttemptResults.justContinue();
        } else {
            LOGGER.info("{} instances were successfully removed from '{}' group", instanceIdsToDetach, asGroupName);
            return AttemptResults.finishWith(Boolean.TRUE);
        }
    }
}
