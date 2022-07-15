package com.sequenceiq.cloudbreak.cloud.template.compute;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public interface DatabaseServerUpgradeService {

    void upgrade(
            AuthenticatedContext ac,
            DatabaseStack stack,
            PersistenceNotifier resourceNotifier,
            TargetMajorVersion databaseVersion) throws Exception;
}
