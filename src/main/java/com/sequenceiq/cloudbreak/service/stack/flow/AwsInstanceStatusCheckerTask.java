package com.sequenceiq.cloudbreak.service.stack.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StackDependentStatusCheckerTask;

@Component
public class AwsInstanceStatusCheckerTask extends StackDependentStatusCheckerTask<AwsInstancesPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceStatusCheckerTask.class);

    @Autowired
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(AwsInstancesPollerObject instances) {
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
    public void handleTimeout(AwsInstancesPollerObject t) {
        throw new InternalServerException(String.format("AWS instances could not reach the desired status: %s on stack: %s", t, t.getStack().getId()));
    }

    @Override
    public String successMessage(AwsInstancesPollerObject t) {
        return String.format("AWS instances successfully reached status: %s on stack: %s", t.getStatus(), t.getStack().getId());
    }

    @Override
    public void handleExit(AwsInstancesPollerObject awsInstances) {
        return;
    }

}
