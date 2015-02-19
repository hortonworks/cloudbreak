package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.List;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.domain.Stack;

public class CloudFormationStackPollerContext {

    private AmazonCloudFormationClient cloudFormationClient;
    private StackStatus successStatus;
    private List<StackStatus> errorStatuses;
    private Stack stack;

    public CloudFormationStackPollerContext(AmazonCloudFormationClient cloudFormationClient, StackStatus successStatus, List<StackStatus> errorStatuses,
            Stack stack) {
        this.cloudFormationClient = cloudFormationClient;
        this.successStatus = successStatus;
        this.errorStatuses = errorStatuses;
        this.stack = stack;
    }

    public AmazonCloudFormationClient getCloudFormationClient() {
        return cloudFormationClient;
    }

    public StackStatus getSuccessStatus() {
        return successStatus;
    }

    public List<StackStatus> getErrorStatuses() {
        return errorStatuses;
    }

    public Stack getStack() {
        return stack;
    }
}
