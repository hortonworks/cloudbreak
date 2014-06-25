package com.sequenceiq.cloudbreak.service.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.event.Event;
import reactor.function.Consumer;

import com.amazonaws.AmazonClientException;
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
import com.sequenceiq.cloudbreak.domain.Stack;

@Service
public class Ec2InstanceRunner implements Consumer<Event<Stack>> {

    private static final int POLLING_INTERVAL = 3000;

    private static final Logger LOGGER = LoggerFactory.getLogger(Ec2InstanceRunner.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private Ec2UserDataBuilder userDataBuilder;

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
                awsStackUtil.createSuccess(stack, ambariIp);
            } else {
                awsStackUtil.createFailed(stack);
            }
        } catch (AmazonClientException e) {
            LOGGER.error("Failed to run EC2 instances", e);
            awsStackUtil.createFailed(stack);
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
            String instanceArn, String email) {
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

        Map<String, String> userDataVariables = new HashMap<>();
        userDataVariables.put("KEYCHAIN", email);
        runInstancesRequest.setUserData(awsStackUtil.encode(userDataBuilder.buildUserData(userDataVariables)));
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
        // TODO: timeout
        boolean stop = false;
        AmbariClient ambariClient = null;
        String ambariServerPublicIp = null;
        LOGGER.info("Starting polling of instance reachability and Ambari server's status (stack: '{}').", stackId);
        while (!stop) {
            awsStackUtil.sleep(POLLING_INTERVAL);
            if (instancesReachable(amazonEC2Client, stackId, instanceIds)) {
                if (ambariClient == null) {
                    ambariServerPublicIp = getAmbariServerIp(amazonEC2Client, instanceIds);
                    LOGGER.info("Ambari server public ip for stack: '{}': '{}'.", stackId, ambariServerPublicIp);
                    ambariClient = new AmbariClient(ambariServerPublicIp);
                }
                try {
                    String ambariHealth = ambariClient.healthCheck();
                    LOGGER.info("Ambari health check returned: {} [stack: '{}']", ambariHealth, stackId);
                    if ("RUNNING".equals(ambariHealth)) {
                        stop = true;
                    }
                } catch (Exception e) {
                    // org.apache.http.conn.HttpHostConnectException
                    LOGGER.error("Ambari unreachable. Trying again in next polling interval.", e);
                }

            }
        }
        return ambariServerPublicIp;
    }

    private boolean instancesReachable(AmazonEC2Client amazonEC2Client, Long stackId, List<String> instanceIds) {
        // TODO: timeout? failed to run?
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
}
