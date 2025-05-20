package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Subnetwork;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.SQLAdmin.Instances.Insert;
import com.google.api.services.sqladmin.model.BackupConfiguration;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.DiskEncryptionConfiguration;
import com.google.api.services.sqladmin.model.IpConfiguration;
import com.google.api.services.sqladmin.model.LocationPreference;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.Settings;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseNetworkView;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerLaunchService;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class GcpDatabaseServerLaunchService extends GcpDatabaseServerBaseService implements DatabaseServerLaunchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseServerLaunchService.class);

    // PostgreSQL server port is fixed for now
    private static final int POSTGRESQL_SERVER_PORT = 5432;

    private static final String GCP_SQL_INSTANCE_PRIVATE_IP_TYPE = "PRIVATE";

    private static final String DEFAULT_ENCRYPTION = "ALLOW_UNENCRYPTED_AND_ENCRYPTED";

    private static final String ENCRYPTED_ONLY = "ENCRYPTED_ONLY";

    @Inject
    private DatabasePollerService databasePollerService;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpDatabaseServerUserService gcpDatabaseServerUserService;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private GcpStackUtil gcpStackUtil;

    public List<CloudResource> launch(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier) throws Exception {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());
        String deploymentName = databaseServerView.getDbServerName();
        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        Compute compute = gcpComputeFactory.buildCompute(ac.getCloudCredential());

        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        List<CloudResource> buildableResource = new ArrayList<>();
        String availabilityZone = ac.getCloudContext().getLocation().getAvailabilityZone().value();
        buildableResource.add(getDatabaseCloudResource(deploymentName, availabilityZone));
        buildableResource.add(getRdsPort(availabilityZone));

        try {
            Optional<DatabaseInstance> databaseInstance = getDatabaseInstance(deploymentName, sqlAdmin, projectId);
            if (databaseInstance.isEmpty()) {
                DatabaseInstance newDatabaseInstance = createDatabaseInstance(stack, deploymentName, compute, projectId);
                Insert insert = sqlAdmin.instances().insert(projectId, newDatabaseInstance);
                insert.setPrettyPrint(Boolean.TRUE);
                try {
                    Operation operation = insert.execute();
                    verifyOperation(operation, buildableResource);
                    CloudResource operationAwareCloudResource = createOperationAwareCloudResource(buildableResource.get(0), operation);
                    databasePollerService.launchDatabasePoller(ac, List.of(operationAwareCloudResource));
                    DatabaseInstance instance = sqlAdmin.instances().get(projectId, deploymentName).execute();
                    if (instance != null) {
                        String instanceName = instance.getName();
                        Builder rdsInstance = CloudResource.builder();
                        buildableResource.add(getRdsHostName(instance, rdsInstance, instanceName, availabilityZone));
                        gcpDatabaseServerUserService.createUser(ac, stack, buildableResource, instanceName);
                    }
                    buildableResource.forEach(dbr -> resourceNotifier.notifyAllocation(dbr, ac.getCloudContext()));
                    return Collections.singletonList(operationAwareCloudResource);
                } catch (GoogleJsonResponseException e) {
                    LOGGER.debug("Error occured in database server launch: {}", e.getMessage());
                    throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
                }
            } else {
                LOGGER.debug("Deployment already exists: {}", deploymentName);
            }
        } catch (GoogleJsonResponseException e) {
            LOGGER.debug("Error occured in database server launch: {}", e.getMessage());
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.get(0).getName());
        }
        return List.of();
    }

    public CloudResource getRdsPort(String availabilityZone) {
        return CloudResource.builder()
                .withType(ResourceType.RDS_PORT)
                .withName(Integer.toString(POSTGRESQL_SERVER_PORT))
                .withAvailabilityZone(availabilityZone)
                .build();
    }

    public CloudResource getRdsHostName(DatabaseInstance instance, Builder rdsInstance, String instanceName, String availabilityZone) {
        return rdsInstance
                .withType(ResourceType.RDS_HOSTNAME)
                .withInstanceId(instanceName)
                .withName(getPrivateIpAddressOfDbInstance(instance, instanceName))
                .withAvailabilityZone(availabilityZone)
                .build();
    }

    private String getPrivateIpAddressOfDbInstance(DatabaseInstance instance, String instanceName) {
        String ipAddress = instance.getIpAddresses()
                .stream()
                .filter(i -> GCP_SQL_INSTANCE_PRIVATE_IP_TYPE.equals(i.getType()))
                .findFirst()
                .orElseThrow(() -> new GcpResourceException(String.format("Private IP address could not be found for database instance '%s'", instanceName)))
                .getIpAddress();
        return ipAddress;
    }

    private DatabaseInstance createDatabaseInstance(DatabaseStack stack, String deploymentName, Compute compute, String projectId) throws java.io.IOException {
        DatabaseServer databaseServer = stack.getDatabaseServer();
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(databaseServer);
        GcpDatabaseNetworkView databaseNetworkView = new GcpDatabaseNetworkView(stack.getNetwork());
        Subnetwork subnetworkForRedbeams;
        if (Strings.isNullOrEmpty(databaseNetworkView.getSharedProjectId())) {
            subnetworkForRedbeams = compute
                    .subnetworks()
                    .get(projectId, databaseServerView.getLocation(), databaseNetworkView.getSubnetId())
                    .execute();
        } else {
            subnetworkForRedbeams = compute
                    .subnetworks()
                    .get(databaseNetworkView.getSharedProjectId(), databaseServerView.getLocation(), databaseNetworkView.getSubnetId())
                    .execute();
        }
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
        databaseInstance.setSettings(getSettings(stack, databaseServerView, subnetworkForRedbeams, databaseNetworkView.getAvailabilityZone()));
        String keyName = databaseServer.getStringParameter(VOLUME_ENCRYPTION_KEY_ID);
        if (keyName != null && !keyName.isEmpty()) {
            DiskEncryptionConfiguration diskEncryptionConfiguration = new DiskEncryptionConfiguration();
            diskEncryptionConfiguration.setKmsKeyName(keyName);
            databaseInstance.setDiskEncryptionConfiguration(diskEncryptionConfiguration);
        }
        return databaseInstance;
    }

    public Settings getSettings(DatabaseStack stack, GcpDatabaseServerView databaseServerView, Subnetwork subnetworkForRedbeams, String availabilityZone) {
        return new Settings()
                .setTier(stack.getDatabaseServer().getFlavor())
                .setActivationPolicy("ALWAYS")
                .setAvailabilityType(stack.getDatabaseServer().getHighAvailability() ? "REGIONAL" : "ZONAL")
                .setStorageAutoResize(true)
                .setDataDiskSizeGb(databaseServerView.getAllocatedStorageInGb())
                .setDataDiskType("PD_SSD")
                .setIpConfiguration(new IpConfiguration()
                        .setPrivateNetwork(subnetworkForRedbeams.getNetwork())
                        .setIpv4Enabled(false)
                        .setSslMode(stack.getDatabaseServer().isUseSslEnforcement() ? ENCRYPTED_ONLY : DEFAULT_ENCRYPTION)
                )
                .setUserLabels(gcpLabelUtil.createLabelsFromTagsMap(stack.getTags()))
                .setBackupConfiguration(
                        new BackupConfiguration()
                                .setEnabled(true)
                                .setBinaryLogEnabled(false)
                )
                .setLocationPreference(new LocationPreference().setZone(availabilityZone))
                .setEdition("ENTERPRISE");
    }
}
