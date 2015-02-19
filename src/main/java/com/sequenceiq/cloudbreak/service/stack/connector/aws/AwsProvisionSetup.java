package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AwsProvisionSetup implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisionSetup.class);

    @Autowired
    private Reactor reactor;

    @Override
    public void setupProvisioning(Stack stack) {
        MDCBuilder.buildMdcContext(stack);

        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, Event.wrap(new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                                .withSetupProperties(new HashMap<String, Object>())
                                .withUserDataParams(getUserDataProperties(stack))
                )
        );
    }

    @Override
    public Optional<String> preProvisionCheck(Stack stack) {
        return Optional.absent();
    }

    @Override
    public Map<String, Object> getSetupProperties(Stack stack) {
        return new HashMap<>();
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
