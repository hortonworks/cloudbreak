package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.BackupConfiguration;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseNetworkView;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerLaunchService;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpDatabaseServerLaunchService extends GcpDatabaseServerBaseService implements DatabaseServerLaunchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseServerLaunchService.class);

    // PostgreSQL server port is fixed for now
    private static final int POSTGRESQL_SERVER_PORT = 5432;

    @Inject
    private DatabasePollerService databasePollerService;

    public List<CloudResource> launch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier) throws Exception {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());
        GcpDatabaseNetworkView databaseNetworkView = new GcpDatabaseNetworkView(stack.getNetwork());
        String deploymentName = databaseServerView.getDbServerName();
        SQLAdmin sqlAdmin = GcpStackUtil.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        String projectId = GcpStackUtil.getProjectId(ac.getCloudCredential());
        List<CloudResource> buildableResource = new ArrayList<>();
        buildableResource.add(
                new CloudResource.Builder()
                        .type(ResourceType.GCP_DATABASE)
                        .name(deploymentName)
                        .build());
        buildableResource.add(
                new CloudResource.Builder()
                        .type(ResourceType.RDS_PORT)
                        .name(Integer.toString(POSTGRESQL_SERVER_PORT))
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
            if (first.isEmpty()) {
                DatabaseInstance databaseInstance = new DatabaseInstance();
                databaseInstance.setCurrentDiskSize(databaseServerView.getAllocatedStorageInMb());
                databaseInstance.setName(deploymentName);
                databaseInstance.setInstanceType("CLOUD_SQL_INSTANCE");
                databaseInstance.setBackendType("SECOND_GEN");
                databaseInstance.setRegion(databaseServerView.getLocation());
                databaseInstance.setRootPassword(databaseServerView.getAdminPassword());
                databaseInstance.setConnectionName(databaseServerView.getAdminLoginName());
                databaseInstance.setGceZone(databaseNetworkView.getAvailabilityZone());
                databaseInstance.setDatabaseVersion(databaseServerView.getDatabaseVersion());
                databaseInstance.setSettings(
                    new Settings()
                        .setTier(stack.getDatabaseServer().getFlavor())
                        .setActivationPolicy("ALWAYS")
                        .setStorageAutoResize(true)
                        .setDataDiskSizeGb(databaseServerView.getAllocatedStorageInGb())
                        .setDataDiskType("PD_SSD")
                        .setBackupConfiguration(
                            new BackupConfiguration()
                                .setEnabled(true)
                                .setBinaryLogEnabled(false)
                        )
                );
                SQLAdmin.Instances.Insert insert = sqlAdmin.instances().insert(projectId, databaseInstance);
                insert.setPrettyPrint(Boolean.TRUE);
                try {
                    Operation operation = insert.execute();
                    verifyOperation(operation, buildableResource);
                    CloudResource operationAwareCloudResource = createOperationAwareCloudResource(buildableResource.get(0), operation);
                    databasePollerService.launchDatabasePoller(ac, List.of(operationAwareCloudResource));
                    DatabaseInstance instance = sqlAdmin.instances().get(projectId, deploymentName).execute();
                    if (instance != null) {
                        buildableResource.add(
                                new CloudResource.Builder()
                                        .type(ResourceType.RDS_HOSTNAME)
                                        .name(instance.getIpAddresses().iterator().next().getIpAddress())
                                        .build()
                        );
                    }
                    buildableResource.forEach(dbr -> resourceNotifier.notifyAllocation(dbr, ac.getCloudContext()));
                    return Collections.singletonList(operationAwareCloudResource);


                } catch (GoogleJsonResponseException e) {
                    throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
                }
            } else {
                LOGGER.debug("Deployment already exists: {}", deploymentName);
            }
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
        return List.of();
    }
}
