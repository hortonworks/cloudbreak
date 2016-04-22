package com.sequenceiq.cloudbreak.cloud.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;

@Service
public class MockSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSetup.class);
    private static final int FINISHED_PROGRESS_VALUE = 100;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, Image image) {
        LOGGER.debug("prepare image has been executed");
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, Image image) {
        return new ImageStatusResult(ImageStatus.CREATE_FINISHED, FINISHED_PROGRESS_VALUE);
    }

    @Override
    public void prerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        LOGGER.debug("setup prerequisites invoked..");
    }

    @Override
    public void validateFileSystem(FileSystem fileSystem) throws Exception {
    }
}
