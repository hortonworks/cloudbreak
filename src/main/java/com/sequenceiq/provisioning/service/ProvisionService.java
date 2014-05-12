package com.sequenceiq.provisioning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.provisioning.controller.json.ProvisionRequestJson;
import com.sequenceiq.provisioning.domain.CloudFormationTemplate;

@Component
public class ProvisionService {

    @Autowired
    private CloudFormationTemplate template;

    public void provisionEC2Cluster(ProvisionRequestJson provisionRequestJson) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(provisionRequestJson.getAccessKey(), provisionRequestJson.getSecretKey());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicAWSCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(provisionRequestJson.getRegion()));

        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(provisionRequestJson.getClusterName())
                .withTemplateBody(template.getBody())
                .withParameters(new Parameter().withParameterKey("KeyName").withParameterValue(provisionRequestJson.getKeyName()));

        CreateStackResult createStackResult = amazonCloudFormationClient.createStack(createStackRequest);
        createStackResult.getStackId();

    }

}
