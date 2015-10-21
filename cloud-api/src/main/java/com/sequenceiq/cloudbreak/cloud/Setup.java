package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;

public interface Setup {

    void prepareImage(AuthenticatedContext authenticatedContext, Image image);

    ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, Image image);

    void execute(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier<ResourcePersisted> persistenceNotifier);

}
