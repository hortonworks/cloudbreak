package com.sequenceiq.cloudbreak.service.stack.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

public class AwsInstanceStatusCheckerTask implements StatusCheckerTask<AwsInstances> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AwsInstances instances) {
        DescribeInstancesResult result = instances.getAmazonEC2Client().describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instances.getInstances()));
        String instancesStatus = instances.getStatus();
        for (Reservation reservation : result.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (!instancesStatus.equalsIgnoreCase(instance.getState().getName())) {
                    MDCBuilder.buildMdcContext(instances.getStack());
                    LOGGER.info("AWS instance is not in {} state, polling stack.", instancesStatus);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(AwsInstances t) {
        throw new InternalServerException(String.format("AWS instances could not reach the desired status: %s on stack: %s", t, t.getStack().getId()));
    }

    @Override
    public String successMessage(AwsInstances t) {
        return String.format("AWS instances successfully reached status: %s on stack: %s", t.getStatus(), t.getStack().getId());
    }

}
