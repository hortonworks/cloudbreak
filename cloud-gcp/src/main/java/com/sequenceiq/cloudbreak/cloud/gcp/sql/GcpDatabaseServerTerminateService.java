package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerTerminateService;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpDatabaseServerTerminateService extends GcpDatabaseServerBaseService implements DatabaseServerTerminateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseServerTerminateService.class);

    @Inject
    private DatabasePollerService databasePollerService;

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public List<CloudResource> terminate(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier) throws Exception {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());
        String deploymentName = databaseServerView.getDbServerName();
        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        List<CloudResource> buildableResource = List.of(
                new CloudResource.Builder()
                .type(ResourceType.GCP_DATABASE)
                .name(deploymentName)
                .build());

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
                SQLAdmin.Instances.Delete delete = sqlAdmin.instances().delete(projectId, deploymentName);
                delete.setPrettyPrint(true);
                try {
                    Operation operation = delete.execute();
                    verifyOperation(operation, buildableResource);
                    CloudResource operationAwareCloudResource = createOperationAwareCloudResource(buildableResource.get(0), operation);
                    databasePollerService.terminateDatabasePoller(ac, buildableResource);
                    return Collections.singletonList(operationAwareCloudResource);
                } catch (GoogleJsonResponseException e) {
                    throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
                }
            }
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
        return List.of();
    }
}
