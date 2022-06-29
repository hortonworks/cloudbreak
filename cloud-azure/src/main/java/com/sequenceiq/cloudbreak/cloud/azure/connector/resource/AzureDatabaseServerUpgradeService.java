package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

@Service
public class AzureDatabaseServerUpgradeService {

    public List<CloudResource> upgrade(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier, TargetMajorVersion databaseVersion)
            throws Exception {
        // TODO: implement this
        return List.of();
    }
}
