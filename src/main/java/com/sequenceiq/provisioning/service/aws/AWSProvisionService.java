package com.sequenceiq.provisioning.service.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.provisioning.controller.json.AWSProvisionResultJson;
import com.sequenceiq.provisioning.controller.json.ProvisionRequestJson;
import com.sequenceiq.provisioning.controller.json.ProvisionResultJson;
import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.service.ProvisionService;

@Component
public class AWSProvisionService implements ProvisionService {

    private static final String OK_STATUS = "ok";

    @Autowired
    private CloudFormationTemplate template;

    @Override
    public ProvisionResultJson provisionCluster(ProvisionRequestJson provisionRequestJson) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(provisionRequestJson.getAccessKey(), provisionRequestJson.getSecretKey());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicAWSCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(provisionRequestJson.getRegion()));

        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(provisionRequestJson.getClusterName())
                .withTemplateBody(template.getBody())
                .withParameters(new Parameter().withParameterKey("KeyName").withParameterValue(provisionRequestJson.getKeyName()));

        CreateStackResult createStackResult = amazonCloudFormationClient.createStack(createStackRequest);
        return new AWSProvisionResultJson(OK_STATUS, createStackResult);
    }

}
