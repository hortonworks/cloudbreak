package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpComputeBaseResourceChecker.OPERATION_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class GcpInstanceStateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceStateChecker.class);

    private static final String ERROR_CODE = "code";

    private final GcpStackUtil gcpStackUtil;

    public GcpInstanceStateChecker(GcpStackUtil gcpStackUtil) {
        this.gcpStackUtil = gcpStackUtil;
    }

    public List<CloudVmInstanceStatus> checkBasedOnOperation(GcpContext context, List<CloudInstance> instances) {
        LOGGER.info("Checking instances('{}') states based on operation id if the operation couldn't be found then fall back to get the instance details.",
                instances.stream().map(CloudInstance::getInstanceId).collect(Collectors.joining(",")));
        List<CloudVmInstanceStatus> result = new ArrayList<>();
        for (CloudInstance instance : instances) {
            String operationName = instance.getStringParameter(OPERATION_ID);
            String availabilityZone = instance.getAvailabilityZone();
            String instanceId = instance.getInstanceId();
            try {
                LOGGER.debug("Get operations of instance('{}'), with operation id:'{}' in availability zone:'{}'", instanceId, operationName, availabilityZone);
                Operation operation = gcpStackUtil.zoneOperation(context.getCompute(), context.getProjectId(), operationName, availabilityZone).execute();
                InstanceStatus status = getInstanceStatusFromOperation(operation);
                LOGGER.info("The status of the instance('{}') based on the related operation('{}') is: {}", instanceId, operationName, status.name());
                result.add(new CloudVmInstanceStatus(instance, status));
            } catch (GoogleJsonResponseException gJsonRespExc) {
                if (resourceNotFoundException(gJsonRespExc)) {
                    LOGGER.info("Operation '{}' could not be found for instance '{}'", operationName, instanceId);
                    CloudVmInstanceStatus vmInstanceStatus = getInstanceStatusFromGcpInstance(context, instance);
                    result.add(vmInstanceStatus);
                } else {
                    String message = String.format("Failed to check the '%s' operation on the instance '%s'.", operationName, instanceId);
                    LOGGER.warn(message, gJsonRespExc);
                    throw new GcpResourceException(message, gJsonRespExc);
                }
            } catch (Exception ex) {
                String message = String.format("Failed to check the '%s' operation on the instance '%s'.", operationName, instanceId);
                LOGGER.warn(message, ex);
                throw new GcpResourceException(message, ex);
            }
        }
        return result;
    }

    private InstanceStatus getInstanceStatusFromOperation(Operation operation) throws Exception {
        InstanceStatus status;
        if (gcpStackUtil.isOperationFinished(operation)) {
            String operationType = operation.getOperationType();
            switch (operationType) {
                case "start":
                case "startWithEncryptionKey":
                    status = InstanceStatus.STARTED;
                    break;
                case "stop":
                    status = InstanceStatus.STOPPED;
                    break;
                case "delete":
                    status = InstanceStatus.TERMINATED;
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Operation type '%s' is not supported by GCP related SPI code!", operationType));
            }
        } else {
            status = InstanceStatus.IN_PROGRESS;
        }
        return status;
    }

    private CloudVmInstanceStatus getInstanceStatusFromGcpInstance(GcpContext context, CloudInstance instance) {
        String instanceId = instance.getInstanceId();
        String projectId = context.getProjectId();
        String availabilityZone = instance.getAvailabilityZone();
        InstanceStatus status;
        try {
            LOGGER.debug("Query instance('{}') details from GCP in availability zone: '{}'", instanceId, availabilityZone);
            Instance gcpInstance = gcpStackUtil.getComputeInstanceWithId(context.getCompute(), projectId, availabilityZone, instanceId);
            status = GcpInstanceStatusMapper.getInstanceStatusFromGcpStatus(gcpInstance.getStatus());
        } catch (GoogleJsonResponseException jsonExc) {
            if (resourceNotFoundException(jsonExc)) {
                LOGGER.info("Instance wit id '{}' could not be found", instanceId);
                status = InstanceStatus.TERMINATED_BY_PROVIDER;
            } else {
                String message = String.format("Failed to fetch the details of the instance '%s' from GCP", instanceId);
                LOGGER.warn(message, jsonExc);
                throw new GcpResourceException(message, jsonExc);
            }
        } catch (IOException e) {
            String message = String.format("Failed to fetch the details of the instance '%s' from GCP.", instanceId);
            LOGGER.warn(message, e);
            throw new GcpResourceException(message, e);
        }
        LOGGER.info("The status of the instance('{}') based on the instance details from GCP is: {}", instanceId, status.name());
        return new CloudVmInstanceStatus(instance, status);
    }

    private boolean resourceNotFoundException(GoogleJsonResponseException ex) {
        return ex.getDetails() != null
                && ex.getDetails().containsKey(ERROR_CODE)
                && (ex.getDetails().get(ERROR_CODE).equals(HttpStatus.SC_NOT_FOUND) || ex.getDetails().get(ERROR_CODE).equals(HttpStatus.SC_FORBIDDEN));
    }
}
