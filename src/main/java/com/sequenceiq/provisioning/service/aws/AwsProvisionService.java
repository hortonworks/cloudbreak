package com.sequenceiq.provisioning.service.aws;

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
import com.sequenceiq.provisioning.controller.InternalServerException;
import com.sequenceiq.provisioning.domain.AwsCredential;
import com.sequenceiq.provisioning.domain.AwsStackDescription;
import com.sequenceiq.provisioning.domain.AwsTemplate;
import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Credential;
import com.sequenceiq.provisioning.domain.DetailedAwsStackDescription;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.Status;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.StackRepository;
import com.sequenceiq.provisioning.service.ProvisionService;
import com.sequenceiq.provisioning.util.FileReaderUtils;

/**
 * Provisions an Ambari based Hadoop cluster on a client's Amazon EC2 account by
 * calling the CloudFormation API with a pre-composed template and with
 * parameters coming from the JSON request. Authentication to the AWS API is
 * established through cross-account session credentials. See
 * {@link CrossAccountCredentialsProvider}.
 */
@Service
public class AwsProvisionService implements ProvisionService {

    private static final String INSTANCE_NAME_TAG = "Name";
    private static final int ONE_SECOND = 1000;
    private static final String INSTANCE_TAG_KEY = "CloudbreakStackId";
    private static final int SESSION_CREDENTIALS_DURATION = 3600;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisionService.class);

    @Autowired
    private CloudFormationTemplate template;

    @Autowired
    private CrossAccountCredentialsProvider credentialsProvider;

    @Autowired
    private StackRepository stackRepository;

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
            stack.setStatus(Status.CREATE_IN_PROGRESS);
            stackRepository.save(stack);
            AmazonCloudFormationClient client = createCloudFormationClient(user, awsTemplate.getRegion(), credential);
            createStack(stack, awsTemplate, client);
            String stackStatus = "CREATE_IN_PROGRESS";
            DescribeStacksResult stackResult = pollStackCreation(stack, client);
            stackStatus = stackResult.getStacks().get(0).getStackStatus();
            if ("CREATE_COMPLETE".equals(stackStatus)) {
                try {
                    AmazonEC2Client amazonEC2Client = createEC2Client(user, awsTemplate.getRegion(), credential);
                    List<String> instanceIds = runInstancesInSubnet(stack, awsTemplate, stackResult, amazonEC2Client);
                    tagInstances(stack, amazonEC2Client, instanceIds);
                    disableSourceDestCheck(amazonEC2Client, instanceIds);
                    boolean ambariLives = pollAmbariServer(amazonEC2Client, instanceIds);
                    if (ambariLives) {
                        setAndSaveStatus(stack, Status.CREATE_COMPLETED);
                    } else {
                        setAndSaveStatus(stack, Status.CREATE_FAILED);
                    }
                } catch (AmazonClientException e) {
                    LOGGER.error("Failed to run EC2 instances", e);
                    setAndSaveStatus(stack, Status.CREATE_FAILED);
                }

            } else {
                LOGGER.error(String.format("Stack creation failed. id: '%s'", stack.getId()));
                setAndSaveStatus(stack, Status.CREATE_FAILED);
            }
        } catch (Throwable t) {
            LOGGER.error("Unhandled exception occured while creating stack on AWS.", t);
            setAndSaveStatus(stack, Status.CREATE_FAILED);
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

    }

    private void setAndSaveStatus(Stack stack, Status status) {
        stack.setStatus(status);
        stackRepository.save(stack);
    }

    private boolean pollAmbariServer(AmazonEC2Client amazonEC2Client, List<String> instanceIds) {
        boolean stop = false;
        while (!stop) {
            sleep(ONE_SECOND);
            if (instancesReachable(amazonEC2Client, instanceIds)) {
                String ambariServerPublicIp = getAmbariServerIp(amazonEC2Client, instanceIds);
                AmbariClient ambariClient = new AmbariClient(ambariServerPublicIp, "8080");
                try {
                    if ("RUNNING".equals(ambariClient.healthCheck())) {
                        stop = true;
                    }
                } catch (Exception e) {
                    // org.apache.http.conn.HttpHostConnectException
                    LOGGER.debug("Ambari unreachable. Trying again in one second.");
                }

            }
        }
        return true;
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
            Thread.sleep(ONE_SECOND);
        } catch (InterruptedException e) {
            throw new InternalServerException("Thread interrupted.", e);
        }
    }

    private boolean instancesReachable(AmazonEC2Client amazonEC2Client, List<String> instanceIds) {
        // TODO: timeout? failed to run?
        boolean instancesReachable = true;
        DescribeInstanceStatusRequest instanceStatusRequest = new DescribeInstanceStatusRequest()
                .withInstanceIds(instanceIds);
        DescribeInstanceStatusResult instanceStatusResult = amazonEC2Client.describeInstanceStatus(instanceStatusRequest);
        if (instanceStatusResult.getInstanceStatuses().size() > 0) {
            for (InstanceStatus status : instanceStatusResult.getInstanceStatuses()) {
                instancesReachable = instancesReachable && "running".equals(status.getInstanceState().getName());
                instancesReachable = instancesReachable && "ok".equals(status.getInstanceStatus().getStatus());
                for (InstanceStatusDetails details : status.getInstanceStatus().getDetails()) {
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
    }

    private List<String> runInstancesInSubnet(Stack stack, AwsTemplate awsTemplate, DescribeStacksResult stackResult, AmazonEC2Client amazonEC2Client) {
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
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest(awsTemplate.getAmiId(), stack.getClusterSize(), stack.getClusterSize());
        runInstancesRequest.setKeyName(awsTemplate.getKeyName());
        runInstancesRequest.setInstanceType(awsTemplate.getInstanceType());
        runInstancesRequest.setUserData(ec2userDataScript);
        IamInstanceProfileSpecification iamInstanceProfileSpecification = new IamInstanceProfileSpecification()
                .withArn("arn:aws:iam::755047402263:instance-profile/readonly-role");
        runInstancesRequest.setIamInstanceProfile(iamInstanceProfileSpecification);

        InstanceNetworkInterfaceSpecification nwIf = new InstanceNetworkInterfaceSpecification()
                .withDeviceIndex(0)
                .withAssociatePublicIpAddress(true)
                .withSubnetId(subnetId)
                .withGroups(securityGroupId);

        runInstancesRequest.setNetworkInterfaces(Arrays.asList(nwIf));
        RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);

        List<String> instanceIds = new ArrayList<>();
        for (Instance instance : runInstancesResult.getReservation().getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }
        return instanceIds;
    }

    private void createStack(Stack stack, AwsTemplate awsTemplate, AmazonCloudFormationClient client) {
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(String.format("%s-%s", stack.getName(), stack.getId()))
                .withTemplateBody(template.getBody())
                .withParameters(new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()));
        client.createStack(createStackRequest);
    }

    private DescribeStacksResult pollStackCreation(Stack stack, AmazonCloudFormationClient client) {
        String stackStatus = "CREATE_IN_PROGRESS";
        DescribeStacksResult stackResult = null;
        while ("CREATE_IN_PROGRESS".equals(stackStatus)) {
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(String.format("%s-%s", stack.getName(), stack.getId()));
            stackResult = client.describeStacks(stackRequest);
            stackStatus = stackResult.getStacks().get(0).getStackStatus();
            sleep(ONE_SECOND);
        }
        return stackResult;
    }

    @Override
    public StackDescription describeStack(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AmazonCloudFormationClient client = createCloudFormationClient(user, awsInfra.getRegion(), credential);
        DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(String.format("%s-%s", stack.getName(), stack.getId()));
        DescribeStacksResult stackResult = client.describeStacks(stackRequest);

        AmazonEC2Client ec2Client = createEC2Client(user, awsInfra.getRegion(), credential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_KEY).withValues(stack.getName()));
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        return new AwsStackDescription(stackResult, instancesResult);
    }

    @Override
    public StackDescription describeStackWithResources(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AmazonCloudFormationClient client = createCloudFormationClient(user, awsInfra.getRegion(), credential);
        DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(String.format("%s-%s", stack.getName(), stack.getId()));
        DescribeStacksResult stackResult = client.describeStacks(stackRequest);

        DescribeStackResourcesRequest resourcesRequest = new DescribeStackResourcesRequest().withStackName(String.format("%s-%s", stack.getName(),
                stack.getId()));
        DescribeStackResourcesResult resourcesResult = client.describeStackResources(resourcesRequest);

        AmazonEC2Client ec2Client = createEC2Client(user, awsInfra.getRegion(), credential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_KEY).withValues(stack.getName()));
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        return new DetailedAwsStackDescription(stackResult, resourcesResult, instancesResult);
    }

    @Override
    public void deleteStack(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();

        AmazonEC2Client ec2Client = createEC2Client(user, awsInfra.getRegion(), credential);
        DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_KEY).withValues(stack.getName()));
        DescribeInstancesResult instancesResult = ec2Client.describeInstances(instancesRequest);

        List<String> instanceIds = new ArrayList<>();
        for (Instance instance : instancesResult.getReservations().get(0).getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }

        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest().withInstanceIds(instanceIds);
        ec2Client.terminateInstances(terminateInstancesRequest);

        AmazonCloudFormationClient client = createCloudFormationClient(user, awsInfra.getRegion(), credential);
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

    private AmazonCloudFormationClient createCloudFormationClient(User user, Regions regions, Credential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari", user, (AwsCredential) credential);
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(regions));
        return amazonCloudFormationClient;
    }

    private AmazonEC2Client createEC2Client(User user, Regions regions, Credential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari", user, (AwsCredential) credential);
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicSessionCredentials);
        amazonEC2Client.setRegion(Region.getRegion(regions));
        return amazonEC2Client;
    }
}
