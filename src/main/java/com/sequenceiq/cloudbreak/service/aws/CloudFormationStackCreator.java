package com.sequenceiq.cloudbreak.service.aws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.Reactor;
import reactor.event.Event;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.event.StackCreationFailure;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

@Service
public class CloudFormationStackCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationStackCreator.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private CloudFormationTemplate cfTemplate;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    public synchronized void createCloudFormationStack(Stack stack, AwsCredential awsCredential, SnsTopic notificationTopic) {
        try {
            Stack currentStack = stackRepository.findById(stack.getId());
            if (currentStack.getStatus().equals(Status.REQUESTED)) {
                currentStack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS);
                websocketService.sendToTopicUser(stack.getUser().getEmail(), WebsocketEndPoint.STACK,
                        new StatusMessage(stack.getId(), currentStack.getName(), currentStack.getStatus().name()));
                AwsTemplate awsTemplate = (AwsTemplate) currentStack.getTemplate();
                AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
                createStack(currentStack, awsTemplate, notificationTopic, client);
            } else {
                LOGGER.info("CloudFormation stack creation was requested for a stack, that is not in REQUESTED status anymore. [stackId: '{}', status: '{}']",
                        currentStack.getId(), currentStack.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack on AWS.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stack.getId());
            StackCreationFailure stackCreationFailure = new StackCreationFailure(stack, "Internal server error occured while creating CloudFormation stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }
    }

    public void startAllRequestedStackCreationForTopic(SnsTopic notificationTopic) {
        AwsCredential awsCredential = notificationTopic.getCredential();
        List<Stack> requestedStacks = stackRepository.findRequestedStacksWithCredential(awsCredential.getId());
        for (Stack stack : requestedStacks) {
            createCloudFormationStack(stack, awsCredential, notificationTopic);
        }
    }

    private void createStack(Stack stack, AwsTemplate awsTemplate, SnsTopic snstopic, AmazonCloudFormationClient client) {
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(cfTemplate.getBody())
                .withNotificationARNs(snstopic.getTopicArn())
                .withParameters(new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()));
        CreateStackResult createStackResult = client.createStack(createStackRequest);
        stack = stackUpdater.updateStackCfAttributes(stack.getId(), stackName, createStackResult.getStackId());
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", stackName, stack.getId());
    }
}
