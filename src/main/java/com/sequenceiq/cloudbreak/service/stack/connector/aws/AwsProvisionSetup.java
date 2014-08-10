package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SnsTopicRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AwsProvisionSetup implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisionSetup.class);

    @Autowired
    private SnsTopicRepository snsTopicRepository;

    @Autowired
    private SnsTopicManager snsTopicManager;

    @Autowired
    private Reactor reactor;

    @Override
    public void setupProvisioning(Stack stack) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();

        SnsTopic snsTopic = snsTopicRepository.findOneForCredentialInRegion(awsCredential.getId(), awsTemplate.getRegion());
        if (snsTopic == null) {
            LOGGER.info("There is no SNS topic created for credential '{}' in region {}. Creating topic now.", awsCredential.getId(),
                    awsTemplate.getRegion().name());
            snsTopicManager.createTopicAndSubscribe(awsCredential, awsTemplate.getRegion());
        } else if (!snsTopic.isConfirmed()) {
            LOGGER.info(
                    "SNS topic found for credential '{}' in region {}, but the subscription is not confirmed. Trying to subscribe again [arn: {}, id: {}]",
                    awsCredential.getId(), awsTemplate.getRegion().name(), snsTopic.getTopicArn(), snsTopic.getId());
            snsTopicManager.subscribeToTopic(awsCredential, awsTemplate.getRegion(), snsTopic.getTopicArn());
        } else {
            LOGGER.info("SNS topic found for credential '{}' in region {}. [arn: {}, id: {}]", awsCredential.getId(), awsTemplate.getRegion().name(),
                    snsTopic.getTopicArn(), snsTopic.getId());
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
            reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, Event.wrap(new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                    .withSetupProperty(SnsTopicManager.NOTIFICATION_TOPIC_ARN_KEY, snsTopic.getTopicArn())));
        }

    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
