package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerCheckerService;

@Service
public class GcpDatabaseServerCheckService extends GcpDatabaseServerBaseService implements DatabaseServerCheckerService {

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Override
    public ExternalDatabaseStatus check(AuthenticatedContext ac, DatabaseStack stack) {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());
        String deploymentName = databaseServerView.getDbServerName();
        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());

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
                switch (first.get().getState()) {
                    case "RUNNABLE":
                        if ("ALWAYS".equals(first.get().getSettings().getActivationPolicy())) {
                            return ExternalDatabaseStatus.STARTED;
                        } else {
                            return ExternalDatabaseStatus.STOPPED;
                        }
                    case "SUSPENDED":
                        return ExternalDatabaseStatus.STOPPED;
                    case "UNKNOWN_STATE":
                    case "FAILED":
                        return ExternalDatabaseStatus.DELETED;
                    default:
                        return ExternalDatabaseStatus.UPDATE_IN_PROGRESS;
                }
            } else {
                return ExternalDatabaseStatus.DELETED;
            }
        } catch (TokenResponseException e) {
            throw gcpStackUtil.getMissingServiceAccountKeyError(e, projectId);
        } catch (IOException ex) {
            throw new CloudConnectorException(ex.getMessage(), ex);
        }
    }
}
