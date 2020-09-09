package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.util.List;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.OperationError;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpDatabaseBaseResourceChecker;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class GcpDatabaseServerBaseService extends AbstractGcpDatabaseBaseResourceChecker {

    protected void verifyOperation(Operation operation, List<CloudResource> buildableResource) {
        if (operation.getError() != null
                && !operation.getError().isEmpty()
                && !operation.getError().getErrors().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (OperationError error : operation.getError().getErrors()) {
                sb.append(error.getMessage() + ",");
            }
            throw new GcpResourceException("Failed to create Database: " + sb.toString(), resourceType(), buildableResource.get(0).getName());
        }
    }

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    protected ResourceType resourceType() {
        return ResourceType.GCP_DATABASE;
    }
}
