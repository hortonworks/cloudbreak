package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.repository.SnsTopicRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.credential.aws.CrossAccountCredentialsProvider;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class SnsTopicManager {

    public static final String NOTIFICATION_TOPIC_ARN_KEY = "notificationTopicArn";

    private static final String CB_TOPIC_NAME = "cloudbreak-notifications";

    private static final Logger LOGGER = LoggerFactory.getLogger(SnsTopicManager.class);

    @Value("${cb.host.addr}")
    private String hostAddress;

    @Value("${cb.sns.ssl}")
    private boolean useSslForSns;

    @Autowired
    private CrossAccountCredentialsProvider credentialsProvider;

    @Autowired
    private SnsTopicRepository snsTopicRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private Reactor reactor;

    public void createTopicAndSubscribe(AwsCredential awsCredential, Regions region) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), awsCredential.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), awsCredential.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.CREDENTIAL_ID.toString());
        AmazonSNSClient amazonSNSClient = awsStackUtil.createSnsClient(region, awsCredential);
        LOGGER.info("Amazon SNS client successfully created.");

        CreateTopicResult createTopicResult = amazonSNSClient.createTopic(CB_TOPIC_NAME);
        LOGGER.info("Amazon SNS topic successfully created. [topic ARN: '{}']", createTopicResult.getTopicArn());

        SnsTopic snsTopic = new SnsTopic();
        snsTopic.setName(CB_TOPIC_NAME);
        snsTopic.setRegion(region);
        snsTopic.setCredential(awsCredential);
        snsTopic.setTopicArn(createTopicResult.getTopicArn());
        snsTopic.setConfirmed(false);
        snsTopicRepository.save(snsTopic);

        subscribeToTopic(amazonSNSClient, createTopicResult.getTopicArn());
    }

    /**
     * Handling subscription confirmation should be done only once, so this
     * method is synchronized.
     */
    public synchronized void confirmSubscription(SnsRequest snsRequest) {
        List<SnsTopic> snsTopics = snsTopicRepository.findByTopicArn(snsRequest.getTopicArn());
        for (SnsTopic snsTopic : snsTopics) {
            if (!snsTopic.isConfirmed()) {
                AmazonSNSClient amazonSNSClient = awsStackUtil.createSnsClient(snsTopic.getRegion(), snsTopic.getCredential());
                ConfirmSubscriptionResult result = amazonSNSClient.confirmSubscription(snsTopic.getTopicArn(), snsRequest.getToken());
                LOGGER.info("Subscription to Amazon SNS topic confirmed. [topic ARN: '{}', subscription ARN: '{}', credential: '{}']",
                        snsRequest.getTopicArn(), result.getSubscriptionArn(), snsTopic.getCredential().getId());
                snsTopic.setConfirmed(true);
                snsTopicRepository.save(snsTopic);
                notifyRequestedStacks(snsTopic);
            }
        }
    }

    private void notifyRequestedStacks(SnsTopic snsTopic) {
        AwsCredential awsCredential = snsTopic.getCredential();
        MDC.put(LoggerContextKey.OWNER_ID.toString(), awsCredential.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), awsCredential.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.CREDENTIAL_ID.toString());
        List<Stack> requestedStacks = stackRepository.findRequestedStacksWithCredential(awsCredential.getId());
        for (Stack stack : requestedStacks) {
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
            reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT,
                    Event.wrap(new ProvisionSetupComplete(CloudPlatform.AWS, stack.getId()).withSetupProperty(NOTIFICATION_TOPIC_ARN_KEY,
                            snsTopic.getTopicArn())));
        }
    }

    public void subscribeToTopic(AwsCredential awsCredential, Regions region, String topicArn) {
        AmazonSNSClient amazonSNSClient = awsStackUtil.createSnsClient(region, awsCredential);
        subscribeToTopic(amazonSNSClient, topicArn);
    }

    private void subscribeToTopic(AmazonSNSClient amazonSNSClient, String topicArn) {
        String subscriptionEndpoint = hostAddress + "/sns";
        if (subscriptionEndpoint.startsWith("https")) {
            if (useSslForSns) {
                amazonSNSClient.subscribe(topicArn, "https", subscriptionEndpoint);
            } else {
                subscriptionEndpoint = subscriptionEndpoint.replaceFirst("https", "http");
                amazonSNSClient.subscribe(topicArn, "http", subscriptionEndpoint);
            }
        } else {
            amazonSNSClient.subscribe(topicArn, "http", subscriptionEndpoint);
        }
        LOGGER.info("Amazon SNS subscription request sent. [topic ARN: '{}', endpoint: '{}']", topicArn, subscriptionEndpoint);
    }

    @VisibleForTesting
    protected void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    @VisibleForTesting
    protected void setUseSslForSns(boolean useSslForSns) {
        this.useSslForSns = useSslForSns;
    }
}
