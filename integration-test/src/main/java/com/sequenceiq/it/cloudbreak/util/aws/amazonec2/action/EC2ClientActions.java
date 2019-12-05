package com.sequenceiq.it.cloudbreak.util.aws.amazonec2.action;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.lambda.model.EC2UnexpectedException;
import com.amazonaws.waiters.FixedDelayStrategy;
import com.amazonaws.waiters.MaxAttemptsRetryStrategy;
import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.WaiterParameters;
import com.amazonaws.waiters.WaiterTimedOutException;
import com.amazonaws.waiters.WaiterUnrecoverableException;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.client.EC2Client;

@Component
public class EC2ClientActions extends EC2Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(EC2ClientActions.class);

    private static final String TERMINATED_STATE = "terminated";

    private static final String STOPPED_STATE = "stopped";

    @Inject
    private SdxUtil sdxUtil;

    public List<String> getInstanceVolumeIds(List<String> instanceIds) {
        AmazonEC2 ec2Client = buildEC2Client();
        DescribeInstancesResult instance = ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));

        List<String> volumeIds = instance.getReservations().get(0).getInstances().get(0).getBlockDeviceMappings().stream()
                .filter(dev -> !"/dev/xvda".equals(dev.getDeviceName()))
                .map(InstanceBlockDeviceMapping::getEbs)
                .map(EbsInstanceBlockDevice::getVolumeId)
                .collect(Collectors.toList());

        Map<String, String> map = IntStream.range(0, instanceIds.size()).boxed()
                .collect(Collectors.toMap(instanceIds::get, volumeIds::get));

        map.forEach((instanceId, volumeId) -> Log.log(LOGGER, format(" Attached volume ID is [%s] for [%s] EC2 instance ", volumeId, instanceId)));
        return volumeIds;
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        AmazonEC2 ec2Client = buildEC2Client();

        TerminateInstancesResult terminateInstancesResult = ec2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        for (String instanceId : instanceIds) {
            try {
                Log.log(LOGGER, format(" EC2 instance [%s] state is [%s] ", instanceId,
                        Objects.requireNonNull(terminateInstancesResult.getTerminatingInstances().stream()
                                .filter(instance -> instance.getInstanceId().equals(instanceId))
                                .findAny().orElse(null)
                        ).getCurrentState().getName()));

                ec2Client.waiters().instanceTerminated().run(new WaiterParameters<DescribeInstancesRequest>(new DescribeInstancesRequest()
                        .withInstanceIds(instanceId))
                        .withPollingStrategy(
                                new PollingStrategy(
                                        new MaxAttemptsRetryStrategy(80), new FixedDelayStrategy(30)
                                )
                        )
                );

                DescribeInstancesResult describeInstanceResult = ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
                InstanceState actualInstanceState = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
                if (TERMINATED_STATE.equals(actualInstanceState.getName())) {
                    Log.log(LOGGER, format(" EC2 Instance: %s state is: %s ", instanceId, TERMINATED_STATE));
                } else {
                    LOGGER.error("EC2 Instance: {} termination has not been successful. So the actual state is: {} ",
                            instanceId, actualInstanceState.getName());
                    throw new TestFailException(" EC2 Instance: " + instanceId
                            + " termination has not been successful, because of the actual state is: "
                            + actualInstanceState.getName());
                }
            } catch (WaiterUnrecoverableException e) {
                LOGGER.error("EC2 Instance {} termination has not been successful, because of WaiterUnrecoverableException: {}", instanceId, e);
            } catch (WaiterTimedOutException e) {
                LOGGER.error("EC2 Instance {} termination has not been successful, because of WaiterTimedOutException: {}", instanceId, e);
            } catch (EC2UnexpectedException e) {
                LOGGER.error("EC2 Instance {} termination has not been successful, because of EC2UnexpectedException: {}", instanceId, e);
            }
        }
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        AmazonEC2 ec2Client = buildEC2Client();

        StopInstancesResult stopInstancesResult = ec2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instanceIds));
        for (String instanceId : instanceIds) {
            try {
                Log.log(LOGGER, format(" EC2 instance [%s] state is [%s] ", instanceId,
                        Objects.requireNonNull(stopInstancesResult.getStoppingInstances().stream()
                                .filter(instance -> instance.getInstanceId().equals(instanceId))
                                .findAny().orElse(null)
                        ).getCurrentState().getName()));

                ec2Client.waiters().instanceStopped().run(new WaiterParameters<DescribeInstancesRequest>(new DescribeInstancesRequest()
                        .withInstanceIds(instanceId))
                        .withPollingStrategy(
                                new PollingStrategy(
                                        new MaxAttemptsRetryStrategy(80), new FixedDelayStrategy(30)
                                )
                        )
                );

                DescribeInstancesResult describeInstanceResult = ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
                InstanceState actualInstanceState = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
                if (STOPPED_STATE.equals(actualInstanceState.getName())) {
                    Log.log(LOGGER, format(" EC2 Instance: %s state is: %s ", instanceId, STOPPED_STATE));
                } else {
                    LOGGER.error("EC2 Instance: {} stop has not been successful. So the actual state is: {} ",
                            instanceId, actualInstanceState.getName());
                    throw new TestFailException(" EC2 Instance: " + instanceId
                            + " stop has not been successful, because of the actual state is: "
                            + actualInstanceState.getName());
                }
            } catch (WaiterUnrecoverableException e) {
                LOGGER.error("EC2 Instance {} stop has not been successful, because of WaiterUnrecoverableException: {}", instanceId, e);
            } catch (WaiterTimedOutException e) {
                LOGGER.error("EC2 Instance {} stop has not been successful, because of WaiterTimedOutException: {}", instanceId, e);
            } catch (EC2UnexpectedException e) {
                LOGGER.error("EC2 Instance {} stop has not been successful, because of EC2UnexpectedException: {}", instanceId, e);
            }
        }
    }
}
