package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.CloudRegion;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public abstract class AbstractGcpResourceBuilder implements CloudPlatformAware {

    protected static final String OPERATION_ID = "opid";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpResourceBuilder.class);

    @Inject
    private GcpResourceNameService resourceNameService;

    @Override
    public String platform() {
        return CloudPlatform.GCP.name();
    }

    @Override
    public String variant() {
        return CloudPlatform.GCP.name();
    }

    public GcpResourceNameService getResourceNameService() {
        return resourceNameService;
    }

    protected List<CloudResourceStatus> checkResources(ResourceType type, GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.info("Check {} resource: {}", type, resource);
            try {
                Operation operation = check(context, resource);
                boolean finished = GcpStackUtil.analyzeOperation(operation);
                ResourceStatus successStatus = context.isBuild() ? ResourceStatus.CREATED : ResourceStatus.DELETED;
                result.add(new CloudResourceStatus(resource, finished ? successStatus : ResourceStatus.IN_PROGRESS));
                if (finished) {
                    if (successStatus == ResourceStatus.CREATED) {
                        LOGGER.info("Creation of {} was successful", resource);
                    } else {
                        LOGGER.info("Deletion of {} was successful", resource);
                    }
                }
            } catch (Exception e) {
                CloudContext cloudContext = auth.getCloudContext();
                throw new GcpResourceException("Error during status check", type,
                        cloudContext.getName(), cloudContext.getId(), resource.getName(), e);
            }
        }
        return result;
    }

    protected Operation check(GcpContext context, DynamicModel resource) throws IOException {
        String operation = resource.getStringParameter(OPERATION_ID);
        try {
            Operation execute = GcpStackUtil.globalOperations(context.getCompute(), context.getProjectId(), operation).execute();
            checkError(execute);
            return execute;
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND)) {
                CloudRegion region = CloudRegion.valueOf(context.getRegion());
                try {
                    Operation execute = GcpStackUtil.regionOperations(context.getCompute(), context.getProjectId(), operation, region).execute();
                    checkError(execute);
                    return execute;
                } catch (GoogleJsonResponseException e1) {
                    if (e1.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND)) {
                        Operation execute = GcpStackUtil.zoneOperations(context.getCompute(), context.getProjectId(), operation, region).execute();
                        checkError(execute);
                        return execute;
                    } else {
                        throw e1;
                    }
                }
            } else {
                throw e;
            }
        }
    }

    protected void checkError(Operation execute) {
        if (execute.getError() != null) {
            String msg = null;
            StringBuilder error = new StringBuilder();
            if (execute.getError().getErrors() != null) {
                for (Operation.Error.Errors errors : execute.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            }
            throw new CloudConnectorException(msg);
        }
    }

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    protected CloudResource createNamedResource(ResourceType type, String name) {
        return new CloudResource.Builder().type(type).name(name).build();
    }

    protected CloudResource createOperationAwareCloudResource(CloudResource resource, Operation operation) {
        return new CloudResource.Builder()
                .cloudResource(resource)
                .params(Collections.<String, Object>singletonMap(OPERATION_ID, operation.getName()))
                .persistent(false)
                .build();
    }

    protected CloudInstance createOperationAwareCloudInstance(CloudInstance instance, Operation operation) {
        return new CloudInstance(instance.getInstanceId(), instance.getTemplate(),
                Collections.<String, Object>singletonMap(OPERATION_ID, operation.getName()));
    }

    protected void exceptionHandler(GoogleJsonResponseException ex, String name, ResourceType resourceType) {
        if (ex.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND)) {
            LOGGER.info("Resource {} not found: {}", resourceType, name);
        } else {
            throw new GcpResourceException(ex.getDetails().getMessage(), ex);
        }
    }

}
