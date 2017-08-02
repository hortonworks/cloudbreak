package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Component(AwsCreateStackStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class AwsCreateStackStatusCheckerTask extends AbstractAwsStackStatusCheckerTask {
    public static final String NAME = "awsCreateStackStatusCheckerTask";

    private final AmazonAutoScalingClient asClient;

    public AwsCreateStackStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cfClient, AmazonAutoScalingClient asClient,
            StackStatus successStatus, StackStatus errorStatus, List<StackStatus> stackErrorStatuses, String cloudFormationStackName) {
        super(authenticatedContext, cfClient, successStatus, errorStatus, stackErrorStatuses, cloudFormationStackName, true);

        this.asClient = asClient;
    }

    @Override
    protected boolean doCheck(Stack cfStack, List<StackEvent> stackEvents) {
        return isSuccess(cfStack, stackEvents);
    }

    @Override
    protected boolean handleError(AmazonServiceException e) {
        throw new CloudConnectorException(getErrorMessage(e.getErrorCode(), e.getErrorMessage()));
    }
}