package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public interface Setup {

    void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack);

    ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack);

    void execute(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier<ResourcePersisted> persistenceNotifier);

}
