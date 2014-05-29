package com.sequenceiq.provisioning.service.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sequenceiq.provisioning.controller.ExceptionControllerAdvice;
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

/**
 * Provisions an Ambari based Hadoop cluster on a client's Amazon EC2 account by
 * calling the CloudFormation API with a pre-composed template and with
 * parameters coming from the JSON request. Authentication to the AWS API is
 * established through cross-account session credentials. See
 * {@link CrossAccountCredentialsProvider}.
 */
@Service
public class AwsProvisionService implements ProvisionService {

    private static final int ONE_SECOND = 1000;
    private static final String INSTANCE_TAG_KEY = "CloudbreakStackId";
    private static final int SESSION_CREDENTIALS_DURATION = 3600;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    @Autowired
    private CloudFormationTemplate template;

    @Autowired
    private CrossAccountCredentialsProvider credentialsProvider;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public void createStack(User user, Stack stack, Credential credential) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();

        stack.setStatus(Status.CREATE_IN_PROGRESS);
        stackRepository.save(stack);

        AmazonCloudFormationClient client = createCloudFormationClient(user,
                awsTemplate.getRegion(), credential);
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(String.format("%s-%s", stack.getName(), stack.getId()))
                .withTemplateBody(template.getBody())
                .withParameters(new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()));
        client.createStack(createStackRequest);

        String stackStatus = "CREATE_IN_PROGRESS";
        DescribeStacksResult stackResult = null;
        while ("CREATE_IN_PROGRESS".equals(stackStatus)) {
            DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(String.format("%s-%s", stack.getName(), stack.getId()));
            stackResult = client.describeStacks(stackRequest);
            stackStatus = stackResult.getStacks().get(0).getStackStatus();
            try {
                Thread.sleep(ONE_SECOND);
            } catch (InterruptedException e) {
                throw new InternalServerException("Thread interrupted.", e);
            }
        }

        if ("CREATE_COMPLETE".equals(stackStatus)) {
            try {
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
                AmazonEC2Client amazonEC2Client = createEC2Client(user, awsTemplate.getRegion(), credential);
                RunInstancesRequest runInstancesRequest = new RunInstancesRequest(awsTemplate.getAmiId(), stack.getClusterSize(), stack.getClusterSize());
                runInstancesRequest.setKeyName(awsTemplate.getKeyName());
                runInstancesRequest.setInstanceType(awsTemplate.getInstanceType());
                runInstancesRequest.setSecurityGroupIds(Arrays.asList(securityGroupId));
                runInstancesRequest.setSubnetId(subnetId);
                // runInstancesRequest.setUserData("");
                RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);

                List<String> instanceIds = new ArrayList<>();
                for (Instance instance : runInstancesResult.getReservation().getInstances()) {
                    instanceIds.add(instance.getInstanceId());
                }

                CreateTagsRequest createTagsRequest = new CreateTagsRequest().withResources(instanceIds)
                        .withTags(new Tag(INSTANCE_TAG_KEY, stack.getName()));
                amazonEC2Client.createTags(createTagsRequest);

                stack.setStatus(Status.CREATE_COMPLETED);
                stackRepository.save(stack);
            } catch (AmazonClientException e) {
                LOGGER.error("Failed to run EC2 instances", e);
                stack.setStatus(Status.CREATE_FAILED);
            }

        } else {
            LOGGER.error(String.format("Stack creation failed. id: '%s'", stack.getId()));
            stack.setStatus(Status.CREATE_FAILED);
            stackRepository.save(stack);
        }

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
