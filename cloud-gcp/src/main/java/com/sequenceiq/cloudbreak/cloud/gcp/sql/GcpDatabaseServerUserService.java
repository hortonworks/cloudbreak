package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.User;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

@Service
public class GcpDatabaseServerUserService extends GcpDatabaseServerBaseService {

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private DatabasePollerService databasePollerService;

    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public void createUser(AuthenticatedContext ac, DatabaseStack stack, List<CloudResource> buildableResource, String instanceName) {
        try {
            String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
            SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());

            User rootUser = getRootUser(stack, projectId, instanceName);
            Operation operation = sqlAdmin.users()
                    .insert(projectId, instanceName, rootUser)
                    .execute();
            verifyOperation(operation, buildableResource);
            CloudResource operationAwareCloudResource = createOperationAwareCloudResource(buildableResource.get(0), operation);
            databasePollerService.insertUserPoller(ac, List.of(operationAwareCloudResource));
        } catch (IOException e) {
            throw new CloudConnectorException(e);
        }
    }

    private User getRootUser(DatabaseStack stack, String projectId, String instanceName) {
        return new User()
                .setProject(projectId)
                .setInstance(instanceName)
                .setName(stack.getDatabaseServer().getRootUserName())
                .setPassword(stack.getDatabaseServer().getRootPassword());
    }

}
