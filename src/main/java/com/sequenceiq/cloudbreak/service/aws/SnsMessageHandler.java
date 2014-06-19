package com.sequenceiq.cloudbreak.service.aws;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.SnsRequest;

@Service
public class SnsMessageHandler {

    private static final String CLOUDFORMATION_SUBJECT = "AWS CloudFormation Notification";

    private static final Logger LOGGER = LoggerFactory.getLogger(SnsMessageHandler.class);

    @Autowired
    private SnsMessageParser snsMessageParser;

    @Autowired
    private SnsTopicManager snsTopicManager;

    public void handleMessage(SnsRequest snsRequest) {
        if (snsRequest.getSubject() != null && CLOUDFORMATION_SUBJECT.equals(snsRequest.getSubject())) {
            Map<String, String> cfMessage = snsMessageParser.parseCFMessage(snsRequest.getMessage());
            if ("AWS::CloudFormation::Stack".equals(cfMessage.get("ResourceType")) && "CREATE_COMPLETE".equals(cfMessage.get("ResourceStatus"))) {
                LOGGER.info("****************************");
                LOGGER.info("We should now notify an internal message consumer, that the stack (with StackId '{}' is ready and the process can go on....",
                        cfMessage.get("StackId"));
                // AwsMessageConsumer.notify(cfMessage.get("StackId"));
                LOGGER.info("****************************");
            }
        } else if ("SubscriptionConfirmation".equals(snsRequest.getType())) {
            snsTopicManager.confirmSubscription(snsRequest);
        }

    }
}
