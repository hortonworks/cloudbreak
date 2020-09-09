package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

public interface DatabaseServerTerminateService {

    List<CloudResource> terminate(
        AuthenticatedContext ac,
        DatabaseStack stack,
        PersistenceNotifier resourceNotifier) throws Exception;
}
