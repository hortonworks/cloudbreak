package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;

@Component
public class AwsSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSetup.class);

    @Inject
    private AwsClient awsClient;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private ResourceNotifier resourceNotifier;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack) {

    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack) {
        return null;
    }

    @Override
    public void execute(AuthenticatedContext authenticatedContext, CloudStack stack) {

    }
}
