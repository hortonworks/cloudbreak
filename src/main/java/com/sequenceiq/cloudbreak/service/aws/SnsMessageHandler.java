package com.sequenceiq.cloudbreak.service.aws;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.Reactor;
import reactor.event.Event;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.event.StackCreationFailure;

@Service
public class SnsMessageHandler {

    private static final String CLOUDFORMATION_SUBJECT = "AWS CloudFormation Notification";

    private static final Logger LOGGER = LoggerFactory.getLogger(SnsMessageHandler.class);

    @Autowired
    private SnsMessageParser snsMessageParser;

    @Autowired
    private SnsTopicManager snsTopicManager;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AwsStackUtil awsStackUtil;

    public void handleMessage(SnsRequest snsRequest) {
        if (isCloudFormationMessage(snsRequest)) {
            Map<String, String> cfMessage = snsMessageParser.parseCFMessage(snsRequest.getMessage());
            if (isStackCreateCompleteMessage(cfMessage)) {
                handleCfStackCreateComplete(cfMessage);
            } else if (isStackFailedMessage(cfMessage)) {
                handleCfStackCreateFailed(cfMessage);
            }
        } else if (isSubscriptionConfirmationMessage(snsRequest)) {
            snsTopicManager.confirmSubscription(snsRequest);
        }
    }

    private boolean isCloudFormationMessage(SnsRequest snsRequest) {
        return snsRequest.getSubject() != null && CLOUDFORMATION_SUBJECT.equals(snsRequest.getSubject());
    }

    private boolean isSubscriptionConfirmationMessage(SnsRequest snsRequest) {
        return "SubscriptionConfirmation".equals(snsRequest.getType());
    }

    private boolean isStackCreateCompleteMessage(Map<String, String> cfMessage) {
        return "AWS::CloudFormation::Stack".equals(cfMessage.get("ResourceType")) && "CREATE_COMPLETE".equals(cfMessage.get("ResourceStatus"));
    }

    private boolean isStackFailedMessage(Map<String, String> cfMessage) {
        return ("AWS::CloudFormation::Stack".equals(cfMessage.get("ResourceType")) && "ROLLBACK_IN_PROGRESS".equals(cfMessage.get("ResourceStatus")))
                || "CREATE_FAILED".equals(cfMessage.get("ResourceStatus"));
    }

    private synchronized void handleCfStackCreateComplete(Map<String, String> cfMessage) {
        Stack stack = stackRepository.findByCfStackId(cfMessage.get("StackId"));
        if (stack == null) {
            LOGGER.info(
                    "Got message that CloudFormation stack created, but no matching stack found in the database [CFStackId: '{}']. Ignoring message.",
                    cfMessage.get("StackId"));
        } else if (!stack.isCfStackCompleted()) {
            stack = stackUpdater.updateCfStackCreateComplete(stack.getId());
            LOGGER.info("CloudFormation stack creation completed. [Id: '{}', CFStackId '{}']", stack.getId(), stack.getCfStackId());
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.CF_STACK_COMPLETED_EVENT, stack.getId());
            reactor.notify(ReactorConfig.CF_STACK_COMPLETED_EVENT, Event.wrap(stack));
        }
    }

    private synchronized void handleCfStackCreateFailed(Map<String, String> cfMessage) {
        Stack stack = stackRepository.findByCfStackId(cfMessage.get("StackId"));
        if (stack == null) {
            LOGGER.info("Got message that CloudFormation stack creation failed, but no matching stack found in the db. [CFStackId: '{}']. Ignoring message.",
                    cfMessage.get("StackId"));
        } else if (!stack.isCfStackCompleted() && !Status.CREATE_FAILED.equals(stack.getStatus())) {
            LOGGER.info("CloudFormation stack creation failed. [Id: '{}', CFStackId '{}']", stack.getId(), stack.getCfStackId());
            StackCreationFailure stackCreationFailure = new StackCreationFailure(stack.getId(), "Error while creating CloudFormation stack: "
                    + cfMessage.get("ResourceStatusReason"));
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stack.getId());
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        } else {
            LOGGER.info("Got message that CloudFormation stack creation failed, but its status is already FAILED [CFStackId: '{}']. Ignoring message.",
                    cfMessage.get("StackId"));
        }
    }
}
