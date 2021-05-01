package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpDatabaseServerStartStopService extends GcpDatabaseServerBaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpDatabaseServerStartStopService.class);

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    protected void startStop(AuthenticatedContext ac, DatabaseStack stack, DatabasePollerService databasePollerService, String policy)
            throws IOException {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());
        String deploymentName = databaseServerView.getDbServerName();
        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());

        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        List<CloudResource> gcpDatabase = getGcpDatabase(stack);

        try {
            InstancesListResponse list = sqlAdmin.instances().list(projectId).execute();
            Optional<DatabaseInstance> first = Optional.empty();
            if (!list.isEmpty()) {
                first = list.getItems()
                        .stream()
                        .filter(e -> e.getName().equals(deploymentName))
                        .findFirst();
            }
            if (!first.isEmpty()) {
                try {
                    Operation operation = sqlAdmin
                            .instances()
                            .patch(projectId, deploymentName, getDatabaseInstance(policy))
                            .execute();
                    verifyOperation(operation, gcpDatabase);
                    CloudResource operationAwareCloudResource = createOperationAwareCloudResource(gcpDatabase.get(0), operation);
                    databasePollerService.startDatabasePoller(ac, List.of(operationAwareCloudResource));
                } catch (GoogleJsonResponseException e) {
                    throw new GcpResourceException(checkException(e), resourceType(), gcpDatabase.get(0).getName());
                }
            } else {
                LOGGER.debug("Deployment does not exists: {}", deploymentName);
            }
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), gcpDatabase.get(0).getName());
        }
    }

    protected DatabaseInstance getDatabaseInstance(String policy) {
        return new DatabaseInstance().setSettings(new Settings().setActivationPolicy(policy));
    }

    protected List<CloudResource> getGcpDatabase(DatabaseStack stack) {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());

        return List.of(new CloudResource.Builder()
                .type(ResourceType.GCP_DATABASE)
                .name(databaseServerView.getDbServerName())
                .build());
    }
}
