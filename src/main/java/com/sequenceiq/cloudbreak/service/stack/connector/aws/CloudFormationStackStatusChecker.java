package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@Component
public class CloudFormationStackStatusChecker implements StatusCheckerTask<CloudFormationStackPollerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationStackStatusChecker.class);

    @Autowired
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(CloudFormationStackPollerContext context) {
        boolean result = false;
        Stack stack = context.getStack();
        StackStatus successStatus = context.getSuccessStatus();
        AmazonCloudFormationClient client = context.getCloudFormationClient();
        Resource resource = stack.getResourceByType(ResourceType.CLOUDFORMATION_STACK);
        String cloudFormationStackName = resource.getResourceName();
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cloudFormationStackName);
        try {
            MDCBuilder.buildMdcContext(stack);
            LOGGER.info("Checking if AWS CloudFormation stack '{}' reached status '{}'", cloudFormationStackName, successStatus);
            DescribeStacksResult describeStacksResult = client.describeStacks(describeStacksRequest);
            List<com.amazonaws.services.cloudformation.model.Stack> stacks = describeStacksResult.getStacks();
            if (stacks.size() > 0) {
                StackStatus actualStatus = StackStatus.valueOf(stacks.get(0).getStackStatus());
                if (context.getErrorStatuses().contains(actualStatus)) {
                    throw new CloudFormationStackException("AWS CloudFormation stack reached an error state: " + actualStatus.toString());
                } else if (actualStatus.equals(successStatus)) {
                    result = true;
                }
            } else {
                throw new CloudFormationStackException("AWS CloudFormation stack could not be found for stack: " + stack.getId());
            }
        } catch (AmazonServiceException e) {
            LOGGER.info("Could not get the description of CloudFormation stack (CB stackId:{}) {}.", stack.getId(), e.getErrorMessage());
            result = true;
        }
        return result;
    }

    @Override
    public void handleTimeout(CloudFormationStackPollerContext context) {
        Stack stack = context.getStack();
        throw new CloudFormationStackException(String.format(
                "AWS CloudFormation stack didn't reach the desired state in the given time frame, stack id: %s, name %s", stack.getId(), stack.getName()));
    }

    @Override
    public String successMessage(CloudFormationStackPollerContext context) {
        Stack stack = context.getStack();
        return String.format("AWS CloudFormation stack(%s) reached the desired state '%s'", stack.getId(), context.getSuccessStatus());
    }

    @Override
    public boolean exitPolling(CloudFormationStackPollerContext cloudFormationStackPollerContext) {
        if (!cloudFormationStackPollerContext.getSuccessStatus().equals(DELETE_COMPLETE)) {
            try {
                Stack byId = stackRepository.findById(cloudFormationStackPollerContext.getStack().getId());
                if (byId == null || byId.getStatus().equals(Status.DELETE_IN_PROGRESS)) {
                    return true;
                }
                return false;
            } catch (Exception ex) {
                return true;
            }
        } else {
            return false;
        }
    }
}