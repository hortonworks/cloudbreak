package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

abstract class AbstractAwsStackStatusCheckerTask extends PollBooleanStateTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAwsStackStatusCheckerTask.class);

    private final AmazonCloudFormationClient cfClient;

    private final StackStatus successStatus;

    private final StackStatus errorStatus;

    private final List<StackStatus> stackErrorStatuses;

    private final String cloudFormationStackName;

    private final DescribeStacksRequest describeStacksRequest;

    private final DescribeStackEventsRequest stackEventsRequest;

    AbstractAwsStackStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cfClient, StackStatus successStatus,
            StackStatus errorStatus, List<StackStatus> stackErrorStatuses, String cloudFormationStackName, boolean cancellable) {
        super(authenticatedContext, cancellable);
        this.cfClient = cfClient;
        this.successStatus = successStatus;
        this.errorStatus = errorStatus;
        this.stackErrorStatuses = stackErrorStatuses;
        this.cloudFormationStackName = cloudFormationStackName;

        describeStacksRequest = new DescribeStacksRequest().withStackName(cloudFormationStackName);
        stackEventsRequest = new DescribeStackEventsRequest().withStackName(cloudFormationStackName);
    }

    @Override
    public Boolean call() {
        LOGGER.info("Checking if AWS CloudFormation stack '{}' reached status '{}'", cloudFormationStackName, successStatus);

        try {
            com.amazonaws.services.cloudformation.model.Stack cfStack = cfClient.describeStacks(describeStacksRequest).getStacks().get(0);
            List<StackEvent> stackEvents = cfClient.describeStackEvents(stackEventsRequest).getStackEvents();
            return doCheck(cfStack, stackEvents);
        } catch (AmazonServiceException e) {
            return handleError(e);
        }
    }

    protected boolean isSuccess(Stack cfStack, List<StackEvent> stackEvents) {
        if (!stackEvents.isEmpty() && cfStack != null) {
            StackStatus cfStackStatus = StackStatus.valueOf(cfStack.getStackStatus());
            if (stackErrorStatuses.contains(cfStackStatus)) {
                throw new CloudConnectorException(getErrorMessage(errorStatus.toString(), getErrorCauseStatusReason(stackEvents, errorStatus)));
            } else if (cfStackStatus.equals(successStatus)) {
                return true;
            }
        }
        return false;
    }

    protected String getErrorMessage(String state, String reason) {
        return String.format("AWS CloudFormation stack reached an error state: %s reason: %s", state, reason);
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
        return cause == null ? "unknown" : cause.getResourceStatusReason();
    }

    abstract boolean doCheck(com.amazonaws.services.cloudformation.model.Stack cfStack, List<StackEvent> stackEvents);

    abstract boolean handleError(AmazonServiceException e);
}
