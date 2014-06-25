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
