package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
        MDCBuilder.buildMdcContext(stack);
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();

        SnsTopic snsTopic = snsTopicRepository.findOneForCredentialInRegion(awsCredential.getId(), Regions.valueOf(stack.getRegion()));
        if (snsTopic == null) {
            LOGGER.info("There is no SNS topic created for credential '{}' in region {}. Creating topic now.", awsCredential.getId(),
                    Regions.valueOf(stack.getRegion()).name());
            snsTopicManager.createTopicAndSubscribe(awsCredential, Regions.valueOf(stack.getRegion()));
        } else if (!snsTopic.isConfirmed()) {
            LOGGER.info(
                    "SNS topic found for credential '{}' in region {}, but the subscription is not confirmed. Trying to subscribe again [arn: {}, id: {}]",
                    awsCredential.getId(), Regions.valueOf(stack.getRegion()).name(), snsTopic.getTopicArn(), snsTopic.getId());
            snsTopicManager.subscribeToTopic(awsCredential, Regions.valueOf(stack.getRegion()), snsTopic.getTopicArn());
        } else {
            LOGGER.info("SNS topic found for credential '{}' in region {}. [arn: {}, id: {}]", awsCredential.getId(), Regions.valueOf(stack.getRegion()).name(),
                    snsTopic.getTopicArn(), snsTopic.getId());
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
            reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, Event.wrap(new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                    .withSetupProperties(getSetupProperties(stack))
                    .withUserDataParams(getUserDataProperties(stack))
                )
            );
        }
    }

    @Override
    public Optional<String> preProvisionCheck(Stack stack) {
        return Optional.absent();
    }

    public Map<String, Object> getSetupProperties(Stack stack) {
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();

        SnsTopic snsTopic = snsTopicRepository.findOneForCredentialInRegion(awsCredential.getId(), Regions.valueOf(stack.getRegion()));
        Map<String, Object> properties = new HashMap<>();
        properties.put(SnsTopicManager.NOTIFICATION_TOPIC_ARN_KEY, snsTopic.getTopicArn());

        return properties;
    }

    @Override
    public Map<String, String> getUserDataProperties(Stack stack) {
        return new HashMap<>();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
