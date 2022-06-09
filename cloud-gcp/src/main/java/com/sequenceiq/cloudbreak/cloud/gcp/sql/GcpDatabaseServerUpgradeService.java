package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerUpgradeService;

@Component
public class GcpDatabaseServerUpgradeService extends GcpDatabaseServerBaseService implements DatabaseServerUpgradeService {
    @Override
    public List<CloudResource> upgrade(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier, String databaseVersion)
            throws Exception {
        return List.of();
    }
}
