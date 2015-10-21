package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(AwsCloudformationStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class AwsCloudformationStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "awsCloudformationStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudformationStatusCheckerTask.class);

    private AmazonCloudFormationClient client;
    private StackStatus successStatus;
    private StackStatus errorStatus;
    private List<StackStatus> stackErrorStatuses;
    private String cloudFormationStackName;

    public AwsCloudformationStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient client,
            StackStatus successStatus, StackStatus errorStatus, List<StackStatus> stackErrorStatuses, String cloudFormationStackName, boolean cancellable) {
        super(authenticatedContext, cancellable);
        this.client = client;
        this.successStatus = successStatus;
        this.errorStatus = errorStatus;
        this.stackErrorStatuses = stackErrorStatuses;
        this.cloudFormationStackName = cloudFormationStackName;
    }

    @Override
    public Boolean call() {
        boolean result = false;
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cloudFormationStackName);
        DescribeStackEventsRequest stackEventsRequest = new DescribeStackEventsRequest().withStackName(cloudFormationStackName);
        LOGGER.info("Checking if AWS CloudFormation stack '{}' reached status '{}'", cloudFormationStackName, successStatus);

        try {
            com.amazonaws.services.cloudformation.model.Stack cfStack = client.describeStacks(describeStacksRequest).getStacks().get(0);
            List<StackEvent> stackEvents = client.describeStackEvents(stackEventsRequest).getStackEvents();
            if (!stackEvents.isEmpty() && cfStack != null) {
                StackStatus cfStackStatus = StackStatus.valueOf(cfStack.getStackStatus());
                if (stackErrorStatuses.contains(cfStackStatus)) {
                    String errorStatusReason = getErrorCauseStatusReason(stackEvents, errorStatus);
                    throw new CloudConnectorException(String.format("AWS CloudFormation stack reached an error state: %s reason: %s", errorStatus.toString(),
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
