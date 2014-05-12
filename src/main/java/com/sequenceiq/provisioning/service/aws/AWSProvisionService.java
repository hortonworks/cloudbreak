package com.sequenceiq.provisioning.service.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.provisioning.controller.json.AWSProvisionResult;
import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;
import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.service.ProvisionService;

@Component
public class AWSProvisionService implements ProvisionService {

    private static final String OK_STATUS = "ok";

    @Autowired
    private CloudFormationTemplate template;

    @Override
    public ProvisionResult provisionCluster(ProvisionRequest provisionRequest) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(provisionRequest.getAccessKey(), provisionRequest.getSecretKey());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicAWSCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(provisionRequest.getRegion()));

        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(provisionRequest.getClusterName())
                .withTemplateBody(template.getBody())
                .withParameters(new Parameter().withParameterKey("KeyName").withParameterValue(provisionRequest.getKeyName()));

        CreateStackResult createStackResult = amazonCloudFormationClient.createStack(createStackRequest);
        return new AWSProvisionResult(OK_STATUS, createStackResult);
    }

}
