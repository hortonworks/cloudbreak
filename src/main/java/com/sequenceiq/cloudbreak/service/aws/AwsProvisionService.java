package com.sequenceiq.cloudbreak.service.aws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
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
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsStackDescription;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedAwsStackDescription;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AmbariClusterInstaller;
import com.sequenceiq.cloudbreak.service.ProvisionService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StackStatusMessage;

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

    private static final String INSTANCE_NAME_TAG = "Name";
    private static final int POLLING_INTERVAL = 3000;
    private static final String INSTANCE_TAG_KEY = "CloudbreakStackId";
    private static final int SESSION_CREDENTIALS_DURATION = 3600;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisionService.class);

    @Autowired
    private CloudFormationTemplate template;

    @Autowired
    private CrossAccountCredentialsProvider credentialsProvider;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private AmbariClusterInstaller ambariClusterInstaller;

    private String ec2userDataScript;

    @PostConstruct
    public void readUserDataScript() throws IOException {
        String content = FileReaderUtils.readFileFromClasspath("ec2-init.sh");
        byte[] encoded = Base64.encodeBase64(content.getBytes());
        ec2userDataScript = new String(encoded);
    }

    @Override
    public void createStack(User user, Stack stack, Credential credential) {
        try {
            AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
            AwsCredential awsCredential = (AwsCredential) credential;
            AmazonCloudFormationClient client = createCloudFormationClient(user, awsTemplate.getRegion(), awsCredential);
            createStack(stack, awsTemplate, client);
            String stackStatus = "CREATE_IN_PROGRESS";
            DescribeStacksResult stackResult = pollStackCreation(stack, client);
            stackStatus = stackResult.getStacks().get(0).getStackStatus();
            if ("CREATE_COMPLETE".equals(stackStatus)) {
                try {
                    AmazonEC2Client amazonEC2Client = createEC2Client(user, awsTemplate.getRegion(), awsCredential);
                    List<String> instanceIds = runInstancesInSubnet(stack, awsTemplate, stackResult, amazonEC2Client, awsCredential.getInstanceProfileRoleArn());
                    tagInstances(stack, amazonEC2Client, instanceIds);
                    disableSourceDestCheck(amazonEC2Client, instanceIds);
                    String ambariIp = pollAmbariServer(amazonEC2Client, stack.getId(), instanceIds);
                    if (ambariIp != null) {
                        createSuccess(stack, ambariIp);
                    } else {
                        createFailed(stack);
                    }
                } catch (AmazonClientException e) {
                    LOGGER.error("Failed to run EC2 instances", e);
                    createFailed(stack);
                }

            } else {
                LOGGER.error(String.format("Stack creation failed. id: '%s'", stack.getId()));
                createFailed(stack);
            }
        } catch (Throwable t) {
            LOGGER.error("Unhandled exception occured while creating stack on AWS.", t);
            createFailed(stack);
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

        LOGGER.debug(String.format("Disabled sourceDestCheck. (instances: '%s', network interfaces: '%s')", instanceIds, enis));

    }

    private void createFailed(Stack stack) {
        stack.setStatus(Status.CREATE_FAILED);
        stackRepository.save(stack);
        websocketService.send("/topic/stack", new StackStatusMessage(stack.getId(), Status.CREATE_FAILED.name()));
    }

    private void createSuccess(Stack stack, String ambariIp) {
        stack.setStatus(Status.CREATE_COMPLETED);
        stack.setAmbariIp(ambariIp);
        stackRepository.save(stack);
        websocketService.send("/topic/stack", new StackStatusMessage(stack.getId(), Status.CREATE_COMPLETED.name()));
        ambariClusterInstaller.installAmbariCluster(stack);

    }

    private String pollAmbariServer(AmazonEC2Client amazonEC2Client, Long stackId, List<String> instanceIds) {
        // TODO: timeout
        boolean stop = false;
        AmbariClient ambariClient = null;
        String ambariServerPublicIp = null;
        LOGGER.debug(String.format("Starting polling of instance reachability and Ambari server's status (stack: '%s').", stackId));
        while (!stop) {
            sleep(POLLING_INTERVAL);
            if (instancesReachable(amazonEC2Client, stackId, instanceIds)) {
                if (ambariClient == null) {
                    ambariServerPublicIp = getAmbariServerIp(amazonEC2Client, instanceIds);
                    LOGGER.debug(String.format("Ambari server public ip for stack: '%s': '%s'.", stackId, ambariServerPublicIp));
                    ambariClient = new AmbariClient(ambariServerPublicIp);
                }
                try {
                    String ambariHealth = ambariClient.healthCheck();
                    LOGGER.debug(String.format("Ambari health check returned: %s", ambariHealth));
                    if ("RUNNING".equals(ambariHealth)) {
                        stop = true;
                    }
                } catch (Exception e) {
                    // org.apache.http.conn.HttpHostConnectException
                    LOGGER.debug("Ambari unreachable. Trying again in next polling interval.");
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
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            throw new InternalServerException("Thread interrupted.", e);
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
                    LOGGER.debug(String.format("Polling instance reachability. (stack id: '%s', instanceId: '%s', status: '%s:%s', '%s:%s')",
                            stackId, status.getInstanceId(),
                            status.getInstanceState().getName(), status.getInstanceStatus().getStatus(),
                            details.getName(), details.getStatus()));
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

    private void tagInstances(Stack stack, AmazonEC2Client amazonEC2Client, List<String> instanceIds) {
        CreateTagsRequest createTagsRequest = new CreateTagsRequest().withResources(instanceIds).withTags(
                new Tag(INSTANCE_TAG_KEY, stack.getName()),
                new Tag(INSTANCE_NAME_TAG, stack.getName()));
        amazonEC2Client.createTags(createTagsRequest);
        LOGGER.debug(String.format("Tagged instances for stack '%s', with Name tag: '%s')", stack.getId(), stack.getName()));
    }

    private List<String> runInstancesInSubnet(Stack stack, AwsTemplate awsTemplate, DescribeStacksResult stackResult, AmazonEC2Client amazonEC2Client,
            String instanceArn) {
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
        runInstancesRequest.setUserData(ec2userDataScript);
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

        LOGGER.debug(String.format("Started instances in subnet created by the CloudFormation stack. (stack: '%s', subnet: '%s')", stack.getId(), subnetId));

        List<String> instanceIds = new ArrayList<>();
        for (Instance instance : runInstancesResult.getReservation().getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }
        LOGGER.debug(String.format("Instances started for stack '%s': '%s')", stack.getId(), instanceIds));
        return instanceIds;
    }

    private void createStack(Stack stack, AwsTemplate awsTemplate, AmazonCloudFormationClient client) {
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(template.getBody())
                .withParameters(new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()));
        client.createStack(createStackRequest);
        LOGGER.debug(String.format("CloudFormation stack creation request sent with stack name: '%s' for stack: '%s'", stackName, stack.getId()));
    }

    private DescribeStacksResult pollStackCreation(Stack stack, AmazonCloudFormationClient client) {
        String stackStatus = "CREATE_IN_PROGRESS";
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        DescribeStacksResult stackResult = null;
        LOGGER.debug(String.format("Starting polling of CloudFormation stack '%s' (stack id: '%s').", stackName, stack.getId()));
        while ("CREATE_IN_PROGRESS".equals(stackStatus)) {
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(stackName);
            stackResult = client.describeStacks(stackRequest);
            stackStatus = stackResult.getStacks().get(0).getStackStatus();
            LOGGER.debug(String.format("Polling CloudFormation stack creation. (stack id: '%s', stackName: '%s', status: '%s')", stack.getId(), stackName,
                    stackStatus));
            sleep(POLLING_INTERVAL);
        }
        return stackResult;
    }

    @Override
    public StackDescription describeStack(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        DescribeStacksResult stackResult = null;
        DescribeInstancesResult instancesResult = null;
        String cfStackName = String.format("%s-%s", stack.getName(), stack.getId());

        try {
            AmazonCloudFormationClient client = createCloudFormationClient(user, awsInfra.getRegion(), awsCredential);
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(cfStackName);
            stackResult = client.describeStacks(stackRequest);
        } catch (AmazonServiceException e) {
            if ("AmazonCloudFormation".equals(e.getServiceName()) && e.getErrorMessage().equals(String.format("Stack:%s does not exist", cfStackName))) {
                LOGGER.error("Amazon CloudFormation stack {} does not exist. Returning null in describeStack.", cfStackName);
                stackResult = new DescribeStacksResult();
            } else {
                throw e;
            }
        }
        AmazonEC2Client ec2Client = createEC2Client(user, awsInfra.getRegion(), awsCredential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_KEY).withValues(stack.getName()));
        instancesResult = ec2Client.describeInstances(instancesRequest);
        return new AwsStackDescription(stackResult, instancesResult);
    }

    @Override
    public StackDescription describeStackWithResources(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        DescribeStacksResult stackResult = null;
        DescribeStackResourcesResult resourcesResult = null;
        String cfStackName = String.format("%s-%s", stack.getName(), stack.getId());

        try {
            AmazonCloudFormationClient client = createCloudFormationClient(user, awsInfra.getRegion(), awsCredential);
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(String.format("%s-%s", stack.getName(), stack.getId()));
            stackResult = client.describeStacks(stackRequest);

            DescribeStackResourcesRequest resourcesRequest = new DescribeStackResourcesRequest().withStackName(String.format("%s-%s", stack.getName(),
                    stack.getId()));
            resourcesResult = client.describeStackResources(resourcesRequest);
        } catch (AmazonServiceException e) {
            if ("AmazonCloudFormation".equals(e.getServiceName()) && e.getErrorMessage().equals(String.format("Stack:%s does not exist", cfStackName))) {
                LOGGER.error("Amazon CloudFormation stack {} does not exist. Returning null in describeStack.", cfStackName);
                stackResult = new DescribeStacksResult();
            } else {
                throw e;
            }
        }

        AmazonEC2Client ec2Client = createEC2Client(user, awsInfra.getRegion(), awsCredential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_KEY).withValues(stack.getName()));
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        return new DetailedAwsStackDescription(stackResult, resourcesResult, instancesResult);
    }

    @Override
    public void deleteStack(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        AmazonEC2Client ec2Client = createEC2Client(user, awsInfra.getRegion(), awsCredential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_KEY).withValues(stack.getName()));
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        if (instancesResult.getReservations().size() > 0) {
            List<String> instanceIds = new ArrayList<>();
            for (Instance instance : instancesResult.getReservations().get(0).getInstances()) {
                instanceIds.add(instance.getInstanceId());
            }
            TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest().withInstanceIds(instanceIds);
            ec2Client.terminateInstances(terminateInstancesRequest);
        }

        AmazonCloudFormationClient client = createCloudFormationClient(user, awsInfra.getRegion(), awsCredential);
        DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(String.format("%s-%s", stack.getName(), stack.getId()));

        client.deleteStack(deleteStackRequest);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public Boolean startAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean stopAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    private AmazonCloudFormationClient createCloudFormationClient(User user, Regions regions, AwsCredential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari", user, credential);
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(regions));
        LOGGER.debug("Amazon CloudFormation client successfully created.");
        return amazonCloudFormationClient;
    }

    private AmazonEC2Client createEC2Client(User user, Regions regions, AwsCredential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari", user, credential);
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicSessionCredentials);
        amazonEC2Client.setRegion(Region.getRegion(regions));
        LOGGER.debug("Amazon EC2 client successfully created.");
        return amazonEC2Client;
    }
}
