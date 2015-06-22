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
public class CloudFormationStackStatusChecker extends StackBasedStatusCheckerTask<CloudFormationStackContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationStackStatusChecker.class);

    @Inject
    private AwsStackUtil awsStackUtil;

    @Inject
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(CloudFormationStackContext context) {
        boolean result = false;
        Stack stack = context.getStack();
        StackStatus successStatus = context.getSuccessStatus();
        StackStatus errorStatus = context.getErrorStatus();
        AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(stack);
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
            LOGGER.warn("Could not get the description of CloudFormation stack: {}", e.getErrorMessage());
            result = true;
        }
        return result;
    }

    @Override
    public void handleTimeout(CloudFormationStackContext context) {
        throw new AwsResourceException(String.format("Timeout while polling AWS CloudFormation stack '%s'", context.getStack().getId()));
    }

    @Override
    public String successMessage(CloudFormationStackContext context) {
        return String.format("CloudFormation stack reached the requested state: %s", context.getSuccessStatus().name());
    }

    @Override
    public boolean exitPolling(CloudFormationStackContext context) {
        if (context.getSuccessStatus().equals(StackStatus.DELETE_COMPLETE)) {
            return false;
        }
        try {
            Stack stack = stackRepository.findById(context.getStack().getId());
            return stack == null || stack.isDeleteInProgress() || stack.isDeleteCompleted();
        } catch (Exception e) {
            return true;
        }
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
}