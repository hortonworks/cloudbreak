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
        ResourceStatus mappedStatus;
        switch (status) {
            case "Ready":
                mappedStatus =  ResourceStatus.UPDATED;
                break;
            case "Canceled":
            case "Failed":
                mappedStatus =  ResourceStatus.FAILED;
                break;
            case "Deleted":
                mappedStatus =  ResourceStatus.DELETED;
                break;
            case "Succeeded":
                mappedStatus =  ResourceStatus.CREATED;
                break;
            case "Accepted":
            case "Running":
            default:
                mappedStatus =  ResourceStatus.IN_PROGRESS;
                break;
        }
        LOGGER.debug("Mapping status {} to resource status {}", status, mappedStatus.toString());
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
        LOGGER.debug("Mapped status {} to common status {}", status, mappedStatus.toString());
        return mappedStatus;
    }
}