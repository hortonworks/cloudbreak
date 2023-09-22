package com.sequenceiq.cloudbreak.cloud.azure.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;

public class AzureStatusMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStatusMapper.class);

    private AzureStatusMapper() {
    }

    public static ResourceStatus mapResourceStatus(String status) {
        ResourceStatus mappedStatus = switch (status) {
            case "Ready" -> ResourceStatus.UPDATED;
            case "Canceled", "Failed" -> ResourceStatus.FAILED;
            case "Deleted" -> ResourceStatus.DELETED;
            case "Succeeded" -> ResourceStatus.CREATED;
            case "Accepted", "Running" -> ResourceStatus.IN_PROGRESS;
            default -> ResourceStatus.IN_PROGRESS;
        };
        LOGGER.debug("Mapping status {} to resource status {}", status, mappedStatus);
        return mappedStatus;
    }

    public static CommonStatus mapCommonStatus(String status) {
        CommonStatus mappedStatus;
        switch (status) {
            case "Accepted":
                mappedStatus = CommonStatus.REQUESTED;
                break;
            case "Ready":
            case "Succeeded":
                mappedStatus = CommonStatus.CREATED;
                break;
            case "Canceled":
            case "Deleted":
            case "Failed":
            default:
                mappedStatus = CommonStatus.FAILED;
                break;
        }
        LOGGER.debug("Mapped status {} to common status {}", status, mappedStatus);
        return mappedStatus;
    }
}