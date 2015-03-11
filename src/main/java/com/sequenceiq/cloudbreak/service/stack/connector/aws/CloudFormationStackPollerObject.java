package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.List;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.domain.Stack;

public class CloudFormationStackPollerObject {

    private AmazonCloudFormationClient cloudFormationClient;
    private StackStatus successStatus;
    private StackStatus errorStatus;
    private List<StackStatus> stackErrorStatuses;
    private Stack stack;

    public CloudFormationStackPollerObject(AmazonCloudFormationClient cloudFormationClient, StackStatus successStatus, StackStatus errorStatus,
            List<StackStatus> stackErrorStatuses, Stack stack) {
        this.cloudFormationClient = cloudFormationClient;
        this.successStatus = successStatus;
        this.errorStatus = errorStatus;
        this.stackErrorStatuses = stackErrorStatuses;
        this.stack = stack;
    }

    public AmazonCloudFormationClient getCloudFormationClient() {
        return cloudFormationClient;
    }

    public StackStatus getSuccessStatus() {
        return successStatus;
    }

    public StackStatus getErrorStatus() {
        return errorStatus;
    }

    public Stack getStack() {
        return stack;
    }

    public List<StackStatus> getStackErrorStatuses() {
        return stackErrorStatuses;
    }
}
