package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public interface DatabaseServerUpgradeService {

    List<CloudResource> upgrade(
            AuthenticatedContext ac,
            DatabaseStack stack,
            PersistenceNotifier resourceNotifier,
            TargetMajorVersion databaseVersion) throws Exception;
}
