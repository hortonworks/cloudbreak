package com.sequenceiq.cloudbreak.service.aws;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Service
public class SnsMessageHandler {

    private static final String CLOUDFORMATION_SUBJECT = "AWS CloudFormation Notification";

    private static final Logger LOGGER = LoggerFactory.getLogger(SnsMessageHandler.class);

    @Autowired
    private SnsMessageParser snsMessageParser;

    @Autowired
    private SnsTopicManager snsTopicManager;

    @Autowired
    private Ec2InstanceRunner ec2InstanceRunner;

    @Autowired
    private StackRepository stackRepository;

    public void handleMessage(SnsRequest snsRequest) {
        if (isCloudFormationMessage(snsRequest)) {
            Map<String, String> cfMessage = snsMessageParser.parseCFMessage(snsRequest.getMessage());
            if (isStackCreateCompleteMessage(cfMessage)) {
                handleCfStackCreateComplete(cfMessage);
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

    private synchronized void handleCfStackCreateComplete(Map<String, String> cfMessage) {
        Stack stack = stackRepository.findByCfStackId(cfMessage.get("StackId"));
        if (!stack.isCfStackCompleted()) {
            LOGGER.info("CloudFormation stack creation completed. (Id: '{}', CFStackId '{}') Notifying instance runner to start instances...",
                    stack.getId(), stack.getCfStackId());
            ec2InstanceRunner.runEc2Instances(stack);
            stack.setCfStackCompleted(true);
            stackRepository.save(stack);
        }
    }
}
