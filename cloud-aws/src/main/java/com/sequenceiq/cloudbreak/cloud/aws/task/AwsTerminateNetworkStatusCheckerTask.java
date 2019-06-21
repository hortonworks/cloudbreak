package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;

@Component(AwsTerminateNetworkStatusCheckerTask.NAME)
@Scope("prototype")
public class AwsTerminateNetworkStatusCheckerTask extends AbstractAwsStackStatusCheckerTask {
    public static final String NAME = "awsTerminateNetworkStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTerminateNetworkStatusCheckerTask.class);

    public AwsTerminateNetworkStatusCheckerTask(AmazonCloudFormationClient cfClient, StackStatus successStatus,
            StackStatus errorStatus, List<StackStatus> stackErrorStatuses, String cloudFormationStackName) {
        super(null, cfClient, successStatus, errorStatus, stackErrorStatuses, cloudFormationStackName, false);
    }

    @Override
    protected boolean doCheck(com.amazonaws.services.cloudformation.model.Stack cfStack, List<StackEvent> stackEvents) {
        return isSuccess(cfStack, stackEvents);
    }

    @Override
    protected boolean handleError(AmazonServiceException e) {
        LOGGER.debug("Could not get the description of CloudFormation stack: {}", e.getErrorMessage());
        return true;
    }
}
