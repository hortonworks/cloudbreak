package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
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
        DescribeInstancesResult result = ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        List<Reservation> reservations = result.getReservations();
        List<Instance> instances = reservations.get(0).getInstances();
        if (CollectionUtils.isEmpty(reservations) || CollectionUtils.isEmpty(instances)) {
            LOGGER.debug("Instance '{}' no longer on provider", instanceId);
            return Boolean.TRUE;
        } else {
            String state = instances.get(0).getState().getName();
            LOGGER.debug("Instance state of '{}' is {}.", instanceId, state);
            return "terminated".equals(state);
        }
    }
}
