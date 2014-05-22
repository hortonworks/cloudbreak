package com.sequenceiq.provisioning.service.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.provisioning.domain.AwsStackDescription;
import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.domain.DetailedAwsStackDescription;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.controller.json.AWSStackResult;
import com.sequenceiq.provisioning.controller.json.StackResult;
import com.sequenceiq.provisioning.domain.AwsTemplate;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
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

    private static final int SESSION_CREDENTIALS_DURATION = 3600;
    private static final String OK_STATUS = "ok";

    @Autowired
    private CloudFormationTemplate template;

    @Autowired
    private CrossAccountCredentialsProvider credentialsProvider;

    @Override
    public StackResult createStack(User user, Stack stack) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        BasicSessionCredentials basicSessionCredentials = credentialsProvider.retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari",
                user.getRoleArn());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(Regions.fromName(awsTemplate.getRegion())));
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(String.format("%s-%s", awsTemplate.getName(), awsTemplate.getId()))
                .withTemplateBody(template.getBody())
                .withParameters(
                        new Parameter().withParameterKey("KeyName").withParameterValue(awsTemplate.getKeyName()),
                        new Parameter().withParameterKey("AMIId").withParameterValue(awsTemplate.getAmiId()),
                        new Parameter().withParameterKey("AmbariAgentCount").withParameterValue(String.valueOf(stack.getClusterSize() - 1)),
                        new Parameter().withParameterKey("ClusterNodeInstanceType").withParameterValue(awsTemplate.getInstanceType().toString()),
                        new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()));
        CreateStackResult createStackResult = amazonCloudFormationClient.createStack(createStackRequest);
        return new AWSStackResult(OK_STATUS, createStackResult);
    }

    @Override
    public StackDescription describeCloudInstance(User user, Stack stack) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        BasicSessionCredentials basicSessionCredentials = credentialsProvider.retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari",
                user.getRoleArn());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(Regions.fromName(awsInfra.getRegion())));
        DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(stack.getName());
        DescribeStacksResult stackResult = amazonCloudFormationClient.describeStacks(stackRequest);
        return new AwsStackDescription(stackResult);
    }

    @Override
    public StackDescription describeCloudInstanceWithResources(User user, Stack stack) {
        AwsTemplate awsInfra = (AwsTemplate) stack.getTemplate();
        BasicSessionCredentials basicSessionCredentials = credentialsProvider.retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari",
                user.getRoleArn());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(Regions.fromName(awsInfra.getRegion())));
        DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(stack.getName());
        DescribeStacksResult stackResult = amazonCloudFormationClient.describeStacks(stackRequest);
        DescribeStackResourcesRequest resourcesRequest = new DescribeStackResourcesRequest().withStackName(stack.getName());
        DescribeStackResourcesResult resourcesResult = amazonCloudFormationClient.describeStackResources(resourcesRequest);
        return new DetailedAwsStackDescription(stackResult, resourcesResult);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
