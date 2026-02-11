package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.User;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

@Component
public class GcpDatabaseServerUpdateService extends GcpDatabaseServerBaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseServerUpdateService.class);

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    public void updateRootUserPassword(AuthenticatedContext ac, DatabaseStack databaseStack, String newPassword) {
        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        DatabaseServer databaseServer = databaseStack.getDatabaseServer();
        String instance = databaseServer.getServerId();
        try {
            LOGGER.info("Update root user password for database: {}", instance);
            User rootUser = sqlAdmin.users().get(projectId, instance, databaseServer.getRootUserName()).execute();
            rootUser.setPassword(newPassword);
            // we need to set to null the new `databaseRoles` field explicitly in user object, otherwise GCP throws HTTP 400
            // https://www.gcpapichanges.com/changes/1763035200-sqladmin:v1.html
            // databaseRoles field is ignored during update, but it seems GCP also validates it to be empty
            rootUser.setDatabaseRoles(null);
            Operation operation = sqlAdmin.users().update(projectId, instance, rootUser)
                    .setName(rootUser.getName())
                    .execute();
            verifyOperation(operation, "root user password");
            LOGGER.info("Root user password updated for database: {}", instance);
        } catch (Exception e) {
            LOGGER.warn("Root user password update failed for database: {}, reason: {}", instance, e.getMessage());
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }
}
