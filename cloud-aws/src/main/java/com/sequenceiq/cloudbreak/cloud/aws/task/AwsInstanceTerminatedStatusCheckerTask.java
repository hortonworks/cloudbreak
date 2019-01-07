package com.sequenceiq.cloudbreak.cloud.aws.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(AwsInstanceTerminatedStatusCheckerTask.NAME)
@Scope("prototype")
public class AwsInstanceTerminatedStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "awsInstanceTerminatedStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceTerminatedStatusCheckerTask.class);

    private final String instanceId;

    private final AmazonEC2Client ec2Client;

    public AwsInstanceTerminatedStatusCheckerTask(AuthenticatedContext authenticatedContext, String instanceId, AmazonEC2Client ec2Client) {
        super(authenticatedContext, true);
        this.instanceId = instanceId;
        this.ec2Client = ec2Client;
    }

    @Override
    protected Boolean doCall() {
        LOGGER.debug("Checking if AWS instance '{}' is terminated.", instanceId);
        DescribeInstanceStatusResult result = ec2Client.describeInstanceStatus(new DescribeInstanceStatusRequest().withInstanceIds(instanceId));
        return CollectionUtils.isEmpty(result.getInstanceStatuses()) || "terminated".equals(result.getInstanceStatuses().get(0).getInstanceState().getName());
    }
}
