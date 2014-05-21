package com.sequenceiq.provisioning.service.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.provisioning.controller.json.AWSCloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.domain.CloudInstance;
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
    public CloudInstanceResult createCloudInstance(User user, CloudInstance cloudInstance) {
        AwsInfra awsInfra = (AwsInfra) cloudInstance.getInfra();
        BasicSessionCredentials basicSessionCredentials = credentialsProvider.retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari",
                user.getRoleArn());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(Regions.fromName(awsInfra.getRegion())));
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(cloudInstance.getName())
                .withTemplateBody(template.getBody())
                .withParameters(
                        new Parameter().withParameterKey("KeyName").withParameterValue(awsInfra.getKeyName()),
                        new Parameter().withParameterKey("AMIId").withParameterValue(awsInfra.getAmiId()),
                        new Parameter().withParameterKey("AmbariAgentCount").withParameterValue(String.valueOf(cloudInstance.getClusterSize() - 1)),
                        new Parameter().withParameterKey("ClusterNodeInstanceType").withParameterValue(awsInfra.getInstanceType().toString()),
                        new Parameter().withParameterKey("SSHLocation").withParameterValue(awsInfra.getSshLocation()));
        CreateStackResult createStackResult = amazonCloudFormationClient.createStack(createStackRequest);
        return new AWSCloudInstanceResult(OK_STATUS, createStackResult);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
