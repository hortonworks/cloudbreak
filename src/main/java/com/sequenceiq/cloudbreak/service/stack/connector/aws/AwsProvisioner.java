package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;

@Component
public class AwsProvisioner implements Provisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisioner.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private CloudFormationTemplateBuilder cfTemplateBuilder;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Override
    public synchronized void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        boolean spotPriced = awsTemplate.getSpotPrice() == null ? false : true;
        List<Parameter> parameters = new ArrayList<>(Arrays.asList(
                new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()),
                new Parameter().withParameterKey("CBUserData").withParameterValue(userData),
                new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                new Parameter().withParameterKey("StackOwner").withParameterValue(awsCredential.getRoleArn()),
                new Parameter().withParameterKey("InstanceCount").withParameterValue(stack.getNodeCount().toString()),
                new Parameter().withParameterKey("InstanceType").withParameterValue(awsTemplate.getInstanceType().toString()),
                new Parameter().withParameterKey("KeyName").withParameterValue(awsCredential.getKeyPairName()),
                new Parameter().withParameterKey("AMI").withParameterValue(awsTemplate.getAmiId()),
                new Parameter().withParameterKey("VolumeSize").withParameterValue(awsTemplate.getVolumeSize().toString()),
                new Parameter().withParameterKey("VolumeType").withParameterValue(awsTemplate.getVolumeType().toString())));
        if (spotPriced) {
            parameters.add(new Parameter().withParameterKey("SpotPrice").withParameterValue(awsTemplate.getSpotPrice().toString()));
        }
        CreateStackRequest createStackRequest = createStackRequest()
                .withStackName(stackName)
                .withTemplateBody(cfTemplateBuilder.build("templates/aws-cf-stack.ftl", awsTemplate.getVolumeCount(), spotPriced))
                .withNotificationARNs((String) setupProperties.get(SnsTopicManager.NOTIFICATION_TOPIC_ARN_KEY))
                .withParameters(parameters);
        CreateStackResult createStackResult = client.createStack(createStackRequest);
        Set<Resource> resources = new HashSet<>();
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, stackName, stack));
        Stack updatedStack = stackUpdater.updateStackResources(stack.getId(), resources);
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", stackName, updatedStack.getId());
    }

    @Override
    public void addNode(Stack stack, String userData) {

    }

    protected CreateStackRequest createStackRequest() {
        return new CreateStackRequest();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
