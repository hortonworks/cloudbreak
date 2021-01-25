package com.sequenceiq.cloudbreak.cloud.model.filesystem.efs;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

// this is from com.amazonaws.services.elasticfilesystem.model.LifeCycleState
public enum LifeCycleState {
    PREPARE,
    CREATING,
    AVAILABLE,
    UPDATING,
    DELETING,
    DELETED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    /**
     * Create instance from input string
     *
     * @param value the input in String to be converted to the enum
     * @return LifeCycleState corresponding state to the input value
     * @throws IllegalArgumentException If the specified value does not map to one of the known values in this enum.
     */
    public static LifeCycleState fromValue(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }

        return valueOf(value.toUpperCase());
    }

    public ResourceStatus toResourceStatus() {
        switch (toString()) {
            case "available":
                return ResourceStatus.CREATED;
            case "deleted":
                return ResourceStatus.DELETED;
            case "creating":
            case "updating":
            case "deleting":
            default:
                return ResourceStatus.IN_PROGRESS;
        }
    }
}
