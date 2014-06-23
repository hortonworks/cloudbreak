package com.sequenceiq.cloudbreak.service.aws;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsStackDescription;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedAwsStackDescription;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.SnsTopicRepository;
import com.sequenceiq.cloudbreak.service.ProvisionService;

/**
 * Provisions an Ambari based Hadoop cluster on a client Amazon EC2 account by
 * calling the CloudFormation API with a pre-composed template to create a VPC
 * with a public subnet, security group and internet gateway with parameters
 * coming from the JSON request. Instances are run by a plain EC2 client after
 * the VPC is ready because CloudFormation cannot handle multiple instances in
 * one reservation group that is currently used by the user data script.
 * Authentication to the AWS API is established through cross-account session
 * credentials. See {@link CrossAccountCredentialsProvider}.
 */
@Service
public class AwsProvisionService implements ProvisionService {

    public static final String INSTANCE_TAG_NAME = "Name";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisionService.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private SnsTopicRepository snsTopicRepository;

    @Autowired
    private SnsTopicManager snsTopicManager;

    @Autowired
    private CloudFormationStackCreator cfStackCreator;

    @Override
    public void createStack(User user, Stack stack, Credential credential) {
        try {
            AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
            AwsCredential awsCredential = (AwsCredential) credential;

            SnsTopic snsTopic = snsTopicRepository.findOneForCredentialInRegion(awsCredential.getId(), awsTemplate.getRegion());
            if (snsTopic == null) {
                LOGGER.info("There is no SNS topic created for credential '{}' in region {}. Creating topic now.", awsCredential.getId(),
                        awsTemplate.getRegion().name());
                snsTopicManager.createTopicAndSubscribe(awsCredential, awsTemplate.getRegion());
            } else if (!snsTopic.isConfirmed()) {
                LOGGER.info(
                        "SNS topic found for credential '{}' in region {}, but the subscription is not confirmed. Trying to subscribe again [arn: {}, id: {}]",
                        awsCredential.getId(), awsTemplate.getRegion().name(), snsTopic.getTopicArn(), snsTopic.getId());
                snsTopicManager.subscribeToTopic(awsCredential, awsTemplate.getRegion(), snsTopic.getTopicArn());
            } else {
                LOGGER.info("SNS topic found for credential '{}' in region {}. [arn: {}, id: {}]", awsCredential.getId(), awsTemplate.getRegion().name(),
                        snsTopic.getTopicArn(), snsTopic.getId());
                cfStackCreator.createCloudFormationStack(stack, awsCredential, snsTopic);
            }
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured when trying to create stack [id: '{}']", stack.getId(), e);
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

    private void createFailed(Stack stack) {
        Stack updatedStack = stackRepository.findById(stack.getId());
        updatedStack.setStatus(Status.CREATE_FAILED);
        stackRepository.save(updatedStack);
        websocketService.sendToTopic(updatedStack.getUser().getId(), "/stack",
                new StatusMessage(updatedStack.getId(), updatedStack.getName(), Status.CREATE_FAILED.name()));
    }

    private void createSuccess(Stack stack, String ambariIp) {
        Stack updatedStack = stackRepository.findById(stack.getId());
        updatedStack.setStatus(Status.CREATE_COMPLETED);
        updatedStack.setAmbariIp(ambariIp);
        stackRepository.save(updatedStack);
        websocketService.sendToTopic(updatedStack.getUser().getId(), "/stack",
                new StatusMessage(updatedStack.getId(), updatedStack.getName(), Status.CREATE_COMPLETED.name()));
        ambariClusterInstaller.installAmbariCluster(updatedStack);

    }

    private String pollAmbariServer(AmazonEC2Client amazonEC2Client, Long stackId, List<String> instanceIds) {
        // TODO: timeout
        boolean stop = false;
        AmbariClient ambariClient = null;
        String ambariServerPublicIp = null;
        LOGGER.info("Starting polling of instance reachability and Ambari server's status (stack: '{}').", stackId);
        while (!stop) {
            sleep(POLLING_INTERVAL);
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
                    LOGGER.info("Ambari unreachable. Trying again in next polling interval.");
                }

            }
        }
        return ambariServerPublicIp;
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

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted exception occured during polling.", e);
            Thread.currentThread().interrupt();
        }
    }

    private boolean instancesReachable(AmazonEC2Client amazonEC2Client, Long stackId, List<String> instanceIds) {
        // TODO: timeout? failed to run?
        boolean instancesReachable = true;
        DescribeInstanceStatusRequest instanceStatusRequest = new DescribeInstanceStatusRequest().withInstanceIds(instanceIds);
        DescribeInstanceStatusResult instanceStatusResult = amazonEC2Client.describeInstanceStatus(instanceStatusRequest);
        if (instanceStatusResult.getInstanceStatuses().size() > 0) {
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
    }

    @Override
    public StackDescription describeStack(User user, Stack stack, Credential credential) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        DescribeStacksResult stackResult = null;
        DescribeInstancesResult instancesResult = null;

        try {
            AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(stack.getCfStackName());
            stackResult = client.describeStacks(stackRequest);
        } catch (AmazonServiceException e) {
            if ("AmazonCloudFormation".equals(e.getServiceName())
                    && e.getErrorMessage().equals(String.format("Stack:%s does not exist", stack.getCfStackName()))) {
                LOGGER.error("Amazon CloudFormation stack {} does not exist. Returning null in describeStack.", stack.getCfStackName());
                stackResult = new DescribeStacksResult();
            } else {
                throw e;
            }
        }
        AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(awsTemplate.getRegion(), awsCredential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_NAME).withValues(stack.getName()));
        instancesResult = ec2Client.describeInstances(instancesRequest);
        return new AwsStackDescription(stackResult, instancesResult);
    }

    @Override
    public StackDescription describeStackWithResources(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        DescribeStacksResult stackResult = null;
        DescribeStackResourcesResult resourcesResult = null;

        try {
            AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsInfra.getRegion(), awsCredential);
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(stack.getCfStackName());
            stackResult = client.describeStacks(stackRequest);

            DescribeStackResourcesRequest resourcesRequest = new DescribeStackResourcesRequest().withStackName(stack.getCfStackName());
            resourcesResult = client.describeStackResources(resourcesRequest);
        } catch (AmazonServiceException e) {
            if ("AmazonCloudFormation".equals(e.getServiceName())
                    && e.getErrorMessage().equals(String.format("Stack:%s does not exist", stack.getCfStackName()))) {
                LOGGER.error("Amazon CloudFormation stack {} does not exist. Returning null in describeStack.", stack.getCfStackName());
                stackResult = new DescribeStacksResult();
            } else {
                throw e;
            }
        }

        AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(awsInfra.getRegion(), awsCredential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_NAME).withValues(stack.getName()));
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        return new DetailedAwsStackDescription(stackResult, resourcesResult, instancesResult);
    }

    @Override
    public void deleteStack(User user, Stack stack, Credential credential) {
        LOGGER.info("Deleting stack: {}", stack.getId(), stack.getCfStackId());
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(awsInfra.getRegion(), awsCredential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_NAME).withValues(stack.getName()));
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        if (!instancesResult.getReservations().isEmpty()) {
            List<String> instanceIds = new ArrayList<>();
            for (Instance instance : instancesResult.getReservations().get(0).getInstances()) {
                instanceIds.add(instance.getInstanceId());
            }
            LOGGER.info("Terminating instances for stack: {} [instances: {}]", stack.getId(), instanceIds);
            TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest().withInstanceIds(instanceIds);
            ec2Client.terminateInstances(terminateInstancesRequest);
        }

        if (stack.getCfStackName() != null) {
            AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsInfra.getRegion(), awsCredential);
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", stack.getId(), stack.getCfStackId());
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(stack.getCfStackName());
            client.deleteStack(deleteStackRequest);
        }
    }

    @Override
    public Boolean startAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean stopAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
