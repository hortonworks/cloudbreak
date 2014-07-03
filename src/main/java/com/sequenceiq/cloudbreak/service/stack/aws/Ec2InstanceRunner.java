package com.sequenceiq.cloudbreak.service.stack.aws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceStatusDetails;
import com.amazonaws.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;
import com.sequenceiq.cloudbreak.service.stack.AmbariServerIpUnavailableException;
import com.sequenceiq.cloudbreak.service.stack.NodeStartTimedOutException;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailure;
import com.sequenceiq.cloudbreak.service.stack.StackCreationSuccess;
import com.sequenceiq.cloudbreak.service.stack.UserDataBuilder;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class Ec2InstanceRunner implements Consumer<Event<Stack>> {

    private static final int POLLING_INTERVAL = 3000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 10;

    private static final String UNHANDLED_EXCEPTION_MSG = "Failed to run EC2 instances";

    private static final Logger LOGGER = LoggerFactory.getLogger(Ec2InstanceRunner.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<Stack> event) {
        Stack stack = event.getData();
        LOGGER.info("Accepted {} event. Starting EC2 instances for stack '{}'", ReactorConfig.CF_STACK_COMPLETED_EVENT, stack.getId());
        try {
            AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
            AwsCredential awsCredential = (AwsCredential) stack.getCredential();

            AmazonCloudFormationClient cfClient = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(stack.getCfStackName());
            DescribeStacksResult stackResult = cfClient.describeStacks(stackRequest);

            AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(awsTemplate.getRegion(), awsCredential);
            List<String> instanceIds = runInstancesInSubnet(stack, awsTemplate, stackResult, ec2Client, awsCredential.getInstanceProfileRoleArn(),
                    awsCredential.getOwner().getEmail());
            tagInstances(stack, ec2Client, instanceIds);
            disableSourceDestCheck(ec2Client, instanceIds);
            String ambariIp = pollAmbariServer(ec2Client, stack.getId(), instanceIds);
            if (ambariIp != null) {
                stackCreateSuccess(stack.getId(), ambariIp);
            } else {
                throw new AmbariServerIpUnavailableException("Couldn't retrieve Ambari server IP.");
            }
        } catch (NodeStartTimedOutException | AmbariServerIpUnavailableException e) {
            LOGGER.error(e.getMessage(), e);
            stackCreateFailed(stack.getId(), e.getMessage());
        } catch (Exception e) {
            LOGGER.error(UNHANDLED_EXCEPTION_MSG, e);
            stackCreateFailed(stack.getId(), UNHANDLED_EXCEPTION_MSG);
        }
    }

    private void disableSourceDestCheck(AmazonEC2Client amazonEC2Client, List<String> instanceIds) {
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);
        List<String> enis = new ArrayList<>();
        for (Instance instance : instancesResult.getReservations().get(0).getInstances()) {
            for (InstanceNetworkInterface instanceNetworkInterface : instance.getNetworkInterfaces()) {
                enis.add(instanceNetworkInterface.getNetworkInterfaceId());
            }
        }

        for (String eni : enis) {
            ModifyNetworkInterfaceAttributeRequest modifyNetworkInterfaceAttributeRequest = new ModifyNetworkInterfaceAttributeRequest()
                    .withNetworkInterfaceId(eni)
                    .withSourceDestCheck(false);
            amazonEC2Client.modifyNetworkInterfaceAttribute(modifyNetworkInterfaceAttributeRequest);
        }

        LOGGER.info("Disabled sourceDestCheck. (instances: '{}', network interfaces: '{}')", instanceIds, enis);

    }

    private void tagInstances(Stack stack, AmazonEC2Client amazonEC2Client, List<String> instanceIds) {
        CreateTagsRequest createTagsRequest = new CreateTagsRequest().withResources(instanceIds).withTags(
                new Tag(AwsProvisionService.INSTANCE_TAG_NAME, stack.getName()));
        amazonEC2Client.createTags(createTagsRequest);
        LOGGER.info("Tagged instances for stack '{}', with Name tag: '{}')", stack.getId(), stack.getName());
    }

    private List<String> runInstancesInSubnet(Stack stack, AwsTemplate awsTemplate, DescribeStacksResult stackResult, AmazonEC2Client amazonEC2Client,
            String instanceArn, String email) throws IOException {
        String subnetId = null;
        String securityGroupId = null;
        List<Output> outputs = stackResult.getStacks().get(0).getOutputs();
        for (Output output : outputs) {
            if ("Subnet".equals(output.getOutputKey())) {
                subnetId = output.getOutputValue();
            } else if ("SecurityGroup".equals(output.getOutputKey())) {
                securityGroupId = output.getOutputValue();
            }
        }
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest(awsTemplate.getAmiId(), stack.getNodeCount(), stack.getNodeCount());
        runInstancesRequest.setKeyName(awsTemplate.getKeyName());
        runInstancesRequest.setInstanceType(awsTemplate.getInstanceType());

        Map<String, String> map = new HashMap<>();
        map.put("KEYCHAIN", email);

        runInstancesRequest.setUserData(awsStackUtil.encode(userDataBuilder.build(CloudPlatform.AWS, map)));
        IamInstanceProfileSpecification iamInstanceProfileSpecification = new IamInstanceProfileSpecification()
                .withArn(instanceArn);
        runInstancesRequest.setIamInstanceProfile(iamInstanceProfileSpecification);

        InstanceNetworkInterfaceSpecification nwIf = new InstanceNetworkInterfaceSpecification()
                .withDeviceIndex(0)
                .withAssociatePublicIpAddress(true)
                .withSubnetId(subnetId)
                .withGroups(securityGroupId);

        runInstancesRequest.setNetworkInterfaces(Arrays.asList(nwIf));
        RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);

        LOGGER.info("Started instances in subnet created by CloudFormation. (stack: '{}', subnet: '{}')", stack.getId(), subnetId);

        List<String> instanceIds = new ArrayList<>();
        for (Instance instance : runInstancesResult.getReservation().getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }
        LOGGER.info("Instances started for stack '{}': '{}')", stack.getId(), instanceIds);
        return instanceIds;
    }

    private String pollAmbariServer(AmazonEC2Client amazonEC2Client, Long stackId, List<String> instanceIds) {
        boolean ambariRunning = false;
        int pollingAttempt = 0;
        AmbariClient ambariClient = null;
        String ambariServerPublicIp = null;
        LOGGER.info("Starting polling of instance reachability and Ambari server's status (stack: '{}').", stackId);
        while (!ambariRunning && !(pollingAttempt >= MAX_POLLING_ATTEMPTS)) {
            if (instancesReachable(amazonEC2Client, stackId, instanceIds)) {
                if (ambariClient == null) {
                    ambariServerPublicIp = getAmbariServerIp(amazonEC2Client, instanceIds);
                    LOGGER.info("Ambari server public ip for stack: '{}': '{}'.", stackId, ambariServerPublicIp);
                    ambariClient = createAmbariClient(ambariServerPublicIp);
                }
                try {
                    String ambariHealth = ambariClient.healthCheck();
                    LOGGER.info("Ambari health check returned: {} [stack: '{}']", ambariHealth, stackId);
                    if ("RUNNING".equals(ambariHealth)) {
                        ambariRunning = true;
                    }
                } catch (Exception e) {
                    LOGGER.error("Ambari unreachable. Trying again in next polling interval.", e);
                }
            }
            awsStackUtil.sleep(POLLING_INTERVAL);
            pollingAttempt++;
        }
        if (pollingAttempt >= MAX_POLLING_ATTEMPTS) {
            throw new AmbariHostsUnavailableException(String.format("Operation timed out. Failed to start all Ambari nodes in %s seconds.",
                    MAX_POLLING_ATTEMPTS * POLLING_INTERVAL / MS_PER_SEC));
        }
        return ambariServerPublicIp;
    }

    private boolean instancesReachable(AmazonEC2Client amazonEC2Client, Long stackId, List<String> instanceIds) {
        boolean instancesReachable = true;
        DescribeInstanceStatusRequest instanceStatusRequest = new DescribeInstanceStatusRequest().withInstanceIds(instanceIds);
        DescribeInstanceStatusResult instanceStatusResult = amazonEC2Client.describeInstanceStatus(instanceStatusRequest);
        if (!instanceStatusResult.getInstanceStatuses().isEmpty()) {
            for (InstanceStatus status : instanceStatusResult.getInstanceStatuses()) {
                instancesReachable = instancesReachable && "running".equals(status.getInstanceState().getName());
                instancesReachable = instancesReachable && "ok".equals(status.getInstanceStatus().getStatus());
                for (InstanceStatusDetails details : status.getInstanceStatus().getDetails()) {
                    LOGGER.info("Polling instance reachability. (stack id: '{}', instanceId: '{}', status: '{}:{}', '{}:{}')",
                            stackId, status.getInstanceId(),
                            status.getInstanceState().getName(), status.getInstanceStatus().getStatus(),
                            details.getName(), details.getStatus());
                    if ("reachability".equals(details.getName())) {
                        instancesReachable = instancesReachable && "passed".equals(details.getStatus());
                    }
                }
            }
        } else {
            instancesReachable = false;
        }
        return instancesReachable;
    }

    private String getAmbariServerIp(AmazonEC2Client amazonEC2Client, List<String> instanceIds) {
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);
        for (Instance instance : instancesResult.getReservations().get(0).getInstances()) {
            if (instance.getAmiLaunchIndex() == 0) {
                return instance.getPublicIpAddress();
            }
        }
        throw new InternalServerException("No instance found with launch index 0");
    }

    private void stackCreateSuccess(Long stackId, String ambariIp) {
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_SUCCESS_EVENT, stackId);
        reactor.notify(ReactorConfig.STACK_CREATE_SUCCESS_EVENT, Event.wrap(new StackCreationSuccess(stackId, ambariIp)));
    }

    private void stackCreateFailed(Long stackId, String message) {
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
        reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new StackCreationFailure(stackId, message)));
    }

    protected AmbariClient createAmbariClient(String ambariServerPublicIp) {
        return new AmbariClient(ambariServerPublicIp);
    }
}
