package com.sequenceiq.it.cloudbreak.util.amazonec2.action;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.amazonec2.client.EC2Client;

@Controller
public class EC2ClientActions extends EC2Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(EC2ClientActions.class);

    private static final String TERMINATED_STATE = "terminated";

    private static final String STOPPED_STATE = "stopped";

    public List<String> getHostGroupVolumeIds(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        AmazonEC2 ec2Client = buildEC2Client();
        List<String> instanceIds = getInstanceIds(testDto, sdxClient, hostGroupName);
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

    /*
    TODO: this code is provider independent. Move it out from AWS specific code.
     */
    public SdxTestDto compareVolumeIdsAfterRepair(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds) {
        actualVolumeIds.sort(Comparator.naturalOrder());
        expectedVolumeIds.sort(Comparator.naturalOrder());

        if (!actualVolumeIds.equals(expectedVolumeIds)) {
            LOGGER.error("Host Group does not have the desired volume IDs!");
            actualVolumeIds.forEach(volumeid -> Log.log(LOGGER, format(" Actual volume ID: %s ", volumeid)));
            expectedVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Desired volume ID: %s ", volumeId)));
            throw new TestFailException("Host Group does not have the desired volume IDs!");
        } else {
            actualVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Before and after SDX repair volume IDs are equal [%s]. ", volumeId)));
        }
        return sdxTestDto;
    }

    public SdxTestDto deleteHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        AmazonEC2 ec2Client = buildEC2Client();
        List<String> instanceIds = getInstanceIds(testDto, sdxClient, hostGroupName);

        TerminateInstancesResult terminateInstancesResult = ec2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        for (String instanceId : instanceIds) {
            try {
                Log.log(LOGGER, format(" [%s] EC2 instance [%s] state is [%s] ", hostGroupName, instanceId,
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
        return testDto;
    }

    public SdxTestDto stopHostGroupInstances(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        AmazonEC2 ec2Client = buildEC2Client();
        List<String> instanceIds = getInstanceIds(testDto, sdxClient, hostGroupName);

        StopInstancesResult stopInstancesResult = ec2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instanceIds));
        for (String instanceId : instanceIds) {
            try {
                Log.log(LOGGER, format(" [%s] EC2 instance [%s] state is [%s] ", hostGroupName, instanceId,
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
        return testDto;
    }

    public SdxInternalTestDto stopHostGroupInstances(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        AmazonEC2 ec2Client = buildEC2Client();
        List<String> instanceIds = getInstanceIds(testDto, sdxClient, hostGroupName);

        StopInstancesResult stopInstancesResult = ec2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instanceIds));
        for (String instanceId : instanceIds) {
            try {
                Log.log(LOGGER, format(" [%s] EC2 instance [%s] state is [%s] ", hostGroupName, instanceId,
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
        return testDto;
    }

    private List<String> getInstanceIds(SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        Set<String> entries = new HashSet<>();
        List<String> instanceIds = new ArrayList<>();

        InstanceGroupV4Response instanceGroupV4Response = sdxClient.getSdxClient().sdxEndpoint().getDetail(testDto.getName(), entries)
                .getStackV4Response().getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                .findFirst()
                .orElse(null);
        InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().findFirst().orElse(null);
        instanceIds.add(Objects.requireNonNull(instanceMetaDataV4Response).getInstanceId());
        return instanceIds;
    }

    private List<String> getInstanceIds(SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        Set<String> entries = new HashSet<>();
        List<String> instanceIds = new ArrayList<>();

        InstanceGroupV4Response instanceGroupV4Response = sdxClient.getSdxClient().sdxEndpoint().getDetail(testDto.getName(), entries)
                .getStackV4Response().getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                .findFirst()
                .orElse(null);
        InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().findFirst().orElse(null);
        instanceIds.add(Objects.requireNonNull(instanceMetaDataV4Response).getInstanceId());
        return instanceIds;
    }
}
