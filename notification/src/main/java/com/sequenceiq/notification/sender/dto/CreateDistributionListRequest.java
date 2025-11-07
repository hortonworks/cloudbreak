package com.sequenceiq.notification.sender.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sequenceiq.notification.domain.EventChannelPreference;

public class CreateDistributionListRequest {

    private String resourceCrn;

    private String resourceName;

    private List<EventChannelPreference> eventChannelPreferences = new ArrayList<>();

    public CreateDistributionListRequest() {
    }

    public CreateDistributionListRequest(String resourceCrn, String resourceName, List<EventChannelPreference> eventChannelPreferences) {
        this.resourceCrn = resourceCrn;
        this.resourceName = resourceName;
        this.eventChannelPreferences = eventChannelPreferences;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public List<EventChannelPreference> getEventChannelPreferences() {
        return eventChannelPreferences;
    }

    public void setEventChannelPreferences(List<EventChannelPreference> eventChannelPreferences) {
        this.eventChannelPreferences = eventChannelPreferences;
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
        return Objects.equals(resourceCrn, that.resourceCrn) &&
                Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(eventChannelPreferences, that.eventChannelPreferences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceCrn, resourceName, eventChannelPreferences);
    }

    @Override
    public String toString() {
        return "CreateDistributionListRequest{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", eventChannelPreferences=" + eventChannelPreferences +
                '}';
    }
}
