package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.util.List;
import java.util.Objects;

import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.OperationInfo;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.OperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class GcpOperationUtil {
    public static final String OPERATION_ID = "opid";

    public static final String OPERATION_INFO = "operationInfo";

    public static final String OPERATION_INFOS = "operationInfos";

    private GcpOperationUtil() {
    }

    public static OperationType getOperationType(Operation operation) {
        if (operation == null) {
            return OperationType.UNKNOWN;
        } else if (operation.getZone() != null) {
            return OperationType.ZONAL;
        } else if (operation.getRegion() != null) {
            return OperationType.REGIONAL;
        } else {
            return OperationType.GLOBAL;
        }
    }

    public static OperationInfo getOperationInfo(CloudResource resource) {
        OperationInfo operationInfo = resource.getParameter(OPERATION_INFO, OperationInfo.class);
        if (operationInfo == null) {
            operationInfo = new OperationInfo(OperationType.UNKNOWN, resource.getStringParameter(OPERATION_ID));
        }
        return operationInfo;
    }

    @SuppressWarnings("unchecked")
    public static List<OperationInfo> getOperationInfos(CloudResource resource) {
        List<OperationInfo> operationInfos = resource.getParameter(OPERATION_INFOS, List.class);
        if (operationInfos == null) {
            List<String> operationIds = Objects.requireNonNullElse(resource.getParameter(OPERATION_ID, List.class), List.of());
            return operationIds.stream()
                    .map(operationId -> new OperationInfo(OperationType.UNKNOWN, operationId))
                    .toList();
        }
        return operationInfos;
    }
}
