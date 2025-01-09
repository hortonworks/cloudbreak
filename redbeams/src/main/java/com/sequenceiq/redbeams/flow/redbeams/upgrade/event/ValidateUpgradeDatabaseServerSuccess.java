package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class ValidateUpgradeDatabaseServerSuccess extends RedbeamsEvent {

    private final List<CloudResourceStatus> resourceList;

    private final String validationWarningMessage;

    @JsonCreator
    public ValidateUpgradeDatabaseServerSuccess(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceList") List<CloudResourceStatus> resourceList,
            @JsonProperty("validationWarningMessage") String validationWarningMessage) {
        super(resourceId);
        this.resourceList = resourceList;
        this.validationWarningMessage = validationWarningMessage;
    }

    public ValidateUpgradeDatabaseServerSuccess(Long resourceId, List<CloudResourceStatus> resourceList) {
        super(resourceId);
        this.resourceList = resourceList;
        this.validationWarningMessage = null;
    }

    public List<CloudResourceStatus> getResourceList() {
        return resourceList;
    }

    public String getValidationWarningMessage() {
        return validationWarningMessage;
    }

    @Override
    public String toString() {
        return "ValidateUpgradeDatabaseServerSuccess{" +
                "resourceList=" + resourceList +
                ", validationWarningMessage='" + validationWarningMessage + '\'' +
                "} " + super.toString();
    }
}