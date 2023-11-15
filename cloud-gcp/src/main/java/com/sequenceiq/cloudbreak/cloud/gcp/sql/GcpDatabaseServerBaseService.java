package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.OperationError;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpDatabaseBaseResourceChecker;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class GcpDatabaseServerBaseService extends AbstractGcpDatabaseBaseResourceChecker {

    protected Optional<DatabaseInstance> getDatabaseInstance(String deploymentName, SQLAdmin sqlAdmin, String projectId) throws IOException {
        InstancesListResponse list = sqlAdmin.instances().list(projectId).execute();
        Optional<DatabaseInstance> databaseInstance = Optional.empty();
        if (!list.isEmpty()) {
            databaseInstance = list.getItems()
                    .stream()
                    .filter(e -> e.getName().equals(deploymentName))
                    .findFirst();
        }
        return databaseInstance;
    }

    protected CloudResource getDatabaseCloudResource(String deploymentName, AuthenticatedContext ac) {
        return getDatabaseCloudResource(deploymentName, ac.getCloudContext().getLocation().getAvailabilityZone().value());
    }

    protected CloudResource getDatabaseCloudResource(String deploymentName, String availabilityZone) {
        return CloudResource.builder()
                .withType(ResourceType.GCP_DATABASE)
                .withName(deploymentName)
                .withAvailabilityZone(availabilityZone)
                .build();
    }

    protected void verifyOperation(Operation operation, List<CloudResource> buildableResource) {
        String resourceName = buildableResource.isEmpty() ? "unknown" : buildableResource.get(0).getName();
        verifyOperation(operation, resourceName);
    }

    protected void verifyOperation(Operation operation, String resourceName) {
        if (operation.getError() != null
                && !operation.getError().isEmpty()
                && !operation.getError().getErrors().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (OperationError error : operation.getError().getErrors()) {
                sb.append(error.getMessage() + ",");
            }
            throw new GcpResourceException("Failed to execute database operation: " + sb.toString(), resourceType(), resourceName);
        }
    }

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    protected ResourceType resourceType() {
        return ResourceType.GCP_DATABASE;
    }
}
