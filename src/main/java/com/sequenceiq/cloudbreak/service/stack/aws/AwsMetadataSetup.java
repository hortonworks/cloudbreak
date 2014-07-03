package com.sequenceiq.cloudbreak.service.stack.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;
import reactor.event.Event;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.StackCreationSuccess;

@Component
public class AwsMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataSetup.class);

    @Autowired
    private Reactor reactor;

    @Override
    public void setupMetadata(Stack stack) {
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SUCCESS_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_SUCCESS_EVENT, Event.wrap(new StackCreationSuccess(stack.getId(), "ambariIp")));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
