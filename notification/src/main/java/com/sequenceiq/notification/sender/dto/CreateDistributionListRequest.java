package com.sequenceiq.notification.sender.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sequenceiq.notification.domain.DistributionListActionType;
import com.sequenceiq.notification.domain.EventChannelPreference;

public class CreateDistributionListRequest {

    private final String parentResourceName;

    private final String parentResourceCrn;

    private final String targetResourceName;

    private final String targetResourceCrn;

    private final List<EventChannelPreference> eventChannelPreferences;

    private final DistributionListActionType actionType;

    private CreateDistributionListRequest(Builder builder) {
        this.parentResourceCrn = builder.parentResourceCrn;
        this.parentResourceName = builder.parentResourceName;
        this.targetResourceName = builder.targetResourceName;
        this.targetResourceCrn = builder.targetResourceCrn;
        this.eventChannelPreferences = builder.eventChannelPreferences;
        this.actionType = builder.actionType;
    }

    public String getParentResourceCrn() {
        return parentResourceCrn;
    }

    public String getParentResourceName() {
        return parentResourceName;
    }

    public String getTargetResourceName() {
        return targetResourceName;
    }

    public String getTargetResourceCrn() {
        return targetResourceCrn;
    }

    public List<EventChannelPreference> getEventChannelPreferences() {
        return eventChannelPreferences;
    }

    public DistributionListActionType getActionType() {
        return actionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreateDistributionListRequest that = (CreateDistributionListRequest) o;
        return Objects.equals(parentResourceCrn, that.parentResourceCrn) &&
                Objects.equals(parentResourceName, that.parentResourceName) &&
                Objects.equals(targetResourceName, that.targetResourceName) &&
                Objects.equals(targetResourceCrn, that.targetResourceCrn) &&
                Objects.equals(eventChannelPreferences, that.eventChannelPreferences) &&
                actionType == that.actionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentResourceCrn, parentResourceName, targetResourceName, targetResourceCrn, eventChannelPreferences, actionType);
    }

    @Override
    public String toString() {
        return "CreateDistributionListRequest{" +
                "resourceCrn='" + parentResourceCrn + '\'' +
                ", resourceName='" + parentResourceName + '\'' +
                ", resourceName='" + targetResourceName + '\'' +
                ", targetResourceCrn='" + targetResourceCrn + '\'' +
                ", eventChannelPreferences=" + eventChannelPreferences +
                ", actionType=" + actionType +
                '}';
    }

    public static class Builder {

        private String parentResourceName;

        private String parentResourceCrn;

        private String targetResourceName;

        private String targetResourceCrn;

        private List<EventChannelPreference> eventChannelPreferences = new ArrayList<>();

        private DistributionListActionType actionType = DistributionListActionType.REGISTRATION;

        public Builder withParentResourceCrn(String parentResourceCrn) {
            this.parentResourceCrn = parentResourceCrn;
            return this;
        }

        public Builder withParentResourceName(String parentResourceName) {
            this.parentResourceName = parentResourceName;
            return this;
        }

        public Builder withTargetResourceName(String targetResourceName) {
            this.targetResourceName = targetResourceName;
            return this;
        }

        public Builder withTargetResourceCrn(String targetResourceCrn) {
            this.targetResourceCrn = targetResourceCrn;
            return this;
        }

        public Builder withEventChannelPreferences(List<EventChannelPreference> eventChannelPreferences) {
            this.eventChannelPreferences = eventChannelPreferences;
            return this;
        }

        public Builder withActionType(DistributionListActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public CreateDistributionListRequest build() {
            return new CreateDistributionListRequest(this);
        }
    }
}
