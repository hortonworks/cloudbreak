package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerUpgradeService;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

@Component
public class GcpDatabaseServerUpgradeService extends GcpDatabaseServerBaseService implements DatabaseServerUpgradeService {
    @Override
    public List<CloudResource> upgrade(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier, TargetMajorVersion databaseVersion)
            throws Exception {
        return List.of();
    }
}