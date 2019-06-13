package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Component(AwsCreateNetworkStatusCheckerTask.NAME)
@Scope("prototype")
public class AwsCreateNetworkStatusCheckerTask extends AbstractAwsStackStatusCheckerTask {

    public static final String NAME = "awsCreateNetworkStatusCheckerTask";

    public AwsCreateNetworkStatusCheckerTask(AmazonCloudFormationClient cfClient, StackStatus successStatus, StackStatus errorStatus,
            List<StackStatus> stackErrorStatuses, String cloudFormationStackName) {
        super(null, cfClient, successStatus, errorStatus, stackErrorStatuses, cloudFormationStackName, true);
    }

    @Override
    protected boolean doCheck(Stack cfStack, List<StackEvent> stackEvents) {
        return isSuccess(cfStack, stackEvents);
    }

    @Override
    protected boolean handleError(AmazonServiceException e) {
        throw new CloudConnectorException(getErrorMessage(e.getErrorCode(), e.getErrorMessage()));
    }

    @Override
    public boolean cancelled() {
        return false;
    }
}
