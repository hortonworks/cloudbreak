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
import com.sequenceiq.provisioning.domain.AwsCloudInstance;
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
        AwsCloudInstance awsCloudInstance = (AwsCloudInstance) cloudInstance;
        Regions region = Regions.fromName(awsCloudInstance.getAwsInfra().getRegion());
        String keyName = awsCloudInstance.getAwsInfra().getKeyName();
        String roleArn = user.getRoleArn();

        BasicSessionCredentials basicSessionCredentials = credentialsProvider.retrieveSessionCredentials(SESSION_CREDENTIALS_DURATION, "provision-ambari",
                roleArn);
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(region));
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(awsCloudInstance.getAwsInfra().getName())
                .withTemplateBody(template.getBody())
                .withParameters(new Parameter().withParameterKey("KeyName").withParameterValue(keyName));
        CreateStackResult createStackResult = amazonCloudFormationClient.createStack(createStackRequest);
        return new AWSCloudInstanceResult(OK_STATUS, createStackResult);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
