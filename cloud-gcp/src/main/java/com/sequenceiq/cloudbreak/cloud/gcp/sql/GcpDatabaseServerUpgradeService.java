package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
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
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerUpgradeService;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

@Component
public class GcpDatabaseServerUpgradeService extends GcpDatabaseServerBaseService implements DatabaseServerUpgradeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseServerUpgradeService.class);

    private static final String POSTGRES = "POSTGRES";

    @Inject
    private DatabasePollerService databasePollerService;

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public void upgrade(AuthenticatedContext ac, DatabaseStack stack, PersistenceNotifier resourceNotifier, TargetMajorVersion databaseVersion)
            throws Exception {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());
        String deploymentName = databaseServerView.getDbServerName();
        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        CloudResource databaseCloudResource = getDatabaseCloudResource(deploymentName, ac);
        List<CloudResource> buildableResource = List.of(databaseCloudResource);

        try {
            Optional<DatabaseInstance> databaseInstance = getDatabaseInstance(deploymentName, sqlAdmin, projectId);
            if (!databaseInstance.isEmpty()) {
                DatabaseInstance dbInstance = databaseInstance.get();
                validateDatabaseInstance(dbInstance);
                Pair<String, Integer> dbEngineAndVersion = parseDatabaseEngineAndVersion(dbInstance.getDatabaseVersion());
                validateDatabaseEngine(dbEngineAndVersion.getLeft());
                if (isDatabaseUpgradeNeeded(dbEngineAndVersion.getRight(), databaseVersion)) {
                    dbInstance.setDatabaseVersion(databaseServerView.getDatabaseType() + "_" + databaseVersion.getMajorVersion());
                    SQLAdmin.Instances.Patch patch = sqlAdmin.instances().patch(projectId, dbInstance.getName(), dbInstance);
                    patch.setPrettyPrint(Boolean.TRUE);
                    Operation operation = patch.execute();
                    verifyOperation(operation, buildableResource);
                    CloudResource operationAwareCloudResource = createOperationAwareCloudResource(databaseCloudResource, operation);
                    databasePollerService.upgradeDatabasePoller(ac, List.of(operationAwareCloudResource));
                    buildableResource.forEach(dbr -> resourceNotifier.notifyUpdate(dbr, ac.getCloudContext()));
                }
            } else {
                String message = "Deployment does not exist: {}" + deploymentName;
                LOGGER.debug(message);
                throw new GcpResourceException(message, resourceType(), databaseCloudResource.getName());
            }
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), databaseCloudResource.getName());
        }
    }

    private void validateDatabaseInstance(DatabaseInstance databaseInstance) {
        if ("RUNNABLE".equals(databaseInstance.getState()) && !"ALWAYS".equals(databaseInstance.getSettings().getActivationPolicy())) {
            String message = "The database instance is not running, upgrade is not possible. current state: " + databaseInstance.getState()
                    + " current activation policy: " + databaseInstance.getSettings().getActivationPolicy();
            LOGGER.warn(message);
            throw new GcpResourceException(message);
        }
    }

    private Pair<String, Integer> parseDatabaseEngineAndVersion(String databaseEngineVersion) {
        Pattern versionRegexPattern = Pattern.compile("^([A-Z]+)_([0-9]+)$");
        Matcher matcher = versionRegexPattern.matcher(databaseEngineVersion);
        if (matcher.matches()) {
            return Pair.of(matcher.group(1), Integer.valueOf(matcher.group(2)));
        } else {
            return Pair.of("UNKNOWN", 0);
        }
    }

    private void validateDatabaseEngine(String databaseEngine) {
        if (!POSTGRES.equals(databaseEngine)) {
            String message = "Database upgrade is not possible for engine: " + databaseEngine + "."
                    + " The only supported engine is " + POSTGRES;
            LOGGER.warn(message);
            throw new GcpResourceException(message);
        }
    }

    private boolean isDatabaseUpgradeNeeded(Integer databaseEngineVersion, TargetMajorVersion targetMajorVersion) {
        Integer targetEngineVersion = Integer.valueOf(targetMajorVersion.getMajorVersion());
        if (databaseEngineVersion.compareTo(targetEngineVersion) > 0) {
            String message = "Database upgrade is not possible from " + databaseEngineVersion + " to " + targetEngineVersion;
            LOGGER.warn(message);
            throw new GcpResourceException(message);
        } else if (databaseEngineVersion.equals(targetEngineVersion)) {
            LOGGER.debug("Database version is up to date: " + targetEngineVersion);
            return false;
        } else {
            LOGGER.debug("Database upgrade is possible and needed from " + databaseEngineVersion + " to " + targetEngineVersion);
            return true;
        }
    }
}
