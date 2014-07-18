package com.sequenceiq.cloudbreak.service.stack.connector.aws;

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
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedAwsStackDescription;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class AwsConnector implements CloudPlatformConnector {

    public static final String INSTANCE_TAG_NAME = "Name";
    private static final String CF_SERVICE_NAME = "AmazonCloudFormation";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConnector.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Override
    public StackDescription describeStackWithResources(User user, Stack stack, Credential credential) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        DescribeStacksResult stackResult = null;
        DescribeStackResourcesResult resourcesResult = null;
        DescribeInstancesResult instancesResult = null;
        Resource resource = stack.getResourceByType(ResourceType.CLOUDFORMATION_STACK);
        if (resource != null) {
            try {
                AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsInfra.getRegion(), awsCredential);
                DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(
                        resource.getResourceName()
                );
                stackResult = client.describeStacks(stackRequest);

                DescribeStackResourcesRequest resourcesRequest = new DescribeStackResourcesRequest().withStackName(
                        resource.getResourceName()
                );
                resourcesResult = client.describeStackResources(resourcesRequest);
            } catch (AmazonServiceException e) {
                if (CF_SERVICE_NAME.equals(e.getServiceName())
                        && e.getErrorMessage().equals(String.format("Stack:%s does not exist",
                        resource.getResourceName()))) {
                    LOGGER.error("Amazon CloudFormation stack {} doesn't exist. Returning null in describeStack.",
                            resource.getResourceName());
                    stackResult = new DescribeStacksResult();
                } else {
                    throw e;
                }
            }
            try {
                AmazonEC2Client ec2Client = awsStackUtil.createEC2Client(awsInfra.getRegion(), awsCredential);
                DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest()
                        .withFilters(new Filter().withName("tag:" + INSTANCE_TAG_NAME).withValues(resource.getResourceName()));
                instancesResult = ec2Client.describeInstances(instancesRequest);
            } catch (Exception ex) {
                instancesResult = new DescribeInstancesResult();
            }

        }
        return new DetailedAwsStackDescription(stackResult, resourcesResult, instancesResult);
    }

    @Override
    public void deleteStack(User user, Stack stack, Credential credential) {
        LOGGER.info("Deleting stack: {}", stack.getId());
        AwsTemplate template = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) credential;
        Resource resource = stack.getResourceByType(ResourceType.CLOUDFORMATION_STACK);
        if (resource != null) {
            AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(template.getRegion(), awsCredential);
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", stack.getId(),
                    resource.getResourceName());
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest()
                    .withStackName(resource.getResourceName());
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
