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
        if (snsRequest.getSubject() != null && CLOUDFORMATION_SUBJECT.equals(snsRequest.getSubject())) {
            Map<String, String> cfMessage = snsMessageParser.parseCFMessage(snsRequest.getMessage());
            if ("AWS::CloudFormation::Stack".equals(cfMessage.get("ResourceType")) && "CREATE_COMPLETE".equals(cfMessage.get("ResourceStatus"))) {
                Stack stack = stackRepository.findByCfStackId(cfMessage.get("StackId"));
                LOGGER.info("CloudFormation stack creation completed. (Id: '{}', CFStackId '{}') Notifying instance runner to start instances...",
                        stack.getId(), stack.getCfStackId());
                ec2InstanceRunner.runEc2Instances(stack);
            }
        } else if ("SubscriptionConfirmation".equals(snsRequest.getType())) {
            snsTopicManager.confirmSubscription(snsRequest);
        }

    }
}
