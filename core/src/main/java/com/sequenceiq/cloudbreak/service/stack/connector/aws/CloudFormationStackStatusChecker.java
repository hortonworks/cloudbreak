package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class CloudFormationStackStatusChecker extends StackBasedStatusCheckerTask<CloudFormationStackPollerObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationStackStatusChecker.class);

    @Inject
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(CloudFormationStackPollerObject context) {
        boolean result = false;
        Stack stack = context.getStack();
        StackStatus successStatus = context.getSuccessStatus();
        StackStatus errorStatus = context.getErrorStatus();
        AmazonCloudFormationClient client = context.getCloudFormationClient();
        String cloudFormationStackName = stack.getResourceByType(ResourceType.CLOUDFORMATION_STACK).getResourceName();
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cloudFormationStackName);
        DescribeStackEventsRequest stackEventsRequest = new DescribeStackEventsRequest().withStackName(cloudFormationStackName);
        LOGGER.info("Checking if AWS CloudFormation stack '{}' reached status '{}'", cloudFormationStackName, successStatus);

        try {
            com.amazonaws.services.cloudformation.model.Stack cfStack = client.describeStacks(describeStacksRequest).getStacks().get(0);
            List<StackEvent> stackEvents = client.describeStackEvents(stackEventsRequest).getStackEvents();
            if (!stackEvents.isEmpty() && cfStack != null) {
                StackStatus cfStackStatus = StackStatus.valueOf(cfStack.getStackStatus());
                if (context.getStackErrorStatuses().contains(cfStackStatus)) {
                    String errorStatusReason = getErrorCauseStatusReason(stackEvents, errorStatus);
                    throw new AwsResourceException(String.format("AWS CloudFormation stack reached an error state: %s reason: %s", errorStatus.toString(),
                            errorStatusReason));
                } else if (cfStackStatus.equals(successStatus)) {
                    result = true;
                }
            }
        } catch (AmazonServiceException e) {
            LOGGER.info("Could not get the description of CloudFormation stack (CB stackId:{}) {}.", stack.getId(), e.getErrorMessage());
            result = true;
        }
        return result;
    }

    private String getErrorCauseStatusReason(List<StackEvent> stackEvents, StackStatus errorStatus) {
        StackEvent cause = null;
        for (StackEvent event : stackEvents) {
            if (event.getResourceStatus().equals(errorStatus.toString())) {
                if (cause == null) {
                    cause = event;
                } else if (cause.getTimestamp().getTime() > event.getTimestamp().getTime()) {
                    cause = event;
                }
            }
        }
        return cause.getResourceStatusReason();
    }

    @Override
    public void handleTimeout(CloudFormationStackPollerObject cloudFormationStackPollerObject) {
        throw new AwsResourceException(String.format(
                "AWS CloudFormation stack didn't reach the desired state in the given time frame, stack id: %s, name %s",
                cloudFormationStackPollerObject.getStack().getId(), cloudFormationStackPollerObject.getStack().getName()));
    }

    @Override
    public String successMessage(CloudFormationStackPollerObject cloudFormationStackPollerObject) {
        return String.format("AWS CloudFormation stack(%s) reached the desired state '%s'", cloudFormationStackPollerObject.getStack().getId(),
                cloudFormationStackPollerObject.getSuccessStatus().name());
    }
}