package com.sequenceiq.cloudbreak.service.stack.aws;

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
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.stack.ProvisionService;

/**
 * Provisions an Ambari based Hadoop cluster on a client Amazon EC2 account by
 * calling the CloudFormation API with a pre-composed template to create a VPC
 * with a public subnet, security group and internet gateway with parameters
 * coming from the JSON request. Instances are run by a plain EC2 client after
 * the VPC is ready because CloudFormation cannot handle multiple instances in
 * one reservation group that is currently used by the user data script.
 * Authentication to the AWS API is established through cross-account session
 * credentials.
 */
@Service
public class AwsProvisionService implements ProvisionService {

    public static final String INSTANCE_TAG_NAME = "Name";
    private static final String CF_SERVICE_NAME = "AmazonCloudFormation";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisionService.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Override
    public void createStack(User user, Stack stack, Credential credential) {
        return;
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
            if (CF_SERVICE_NAME.equals(e.getServiceName())
                    && e.getErrorMessage().equals(String.format("Stack:%s doesn't exist", stack.getCfStackName()))) {
                LOGGER.error("Amazon CloudFormation stack {} doesn't exist. Returning null in describeStack.", stack.getCfStackName());
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
            if (CF_SERVICE_NAME.equals(e.getServiceName())
                    && e.getErrorMessage().equals(String.format("Stack:%s doesn't exist", stack.getCfStackName()))) {
                LOGGER.error("Amazon CloudFormation stack {} doesn't exist. Returning null in describeStack.", stack.getCfStackName());
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
