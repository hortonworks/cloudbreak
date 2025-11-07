package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CreateOrUpdateDistributionListRequestDto {

    private String resourceCrn;

    private String resourceName;

    private List<EventChannelPreferenceDto> eventChannelPreferences = new ArrayList<>();

    private Set<String> emailAddresses = new HashSet<>();

    private String distributionListId;

    private String parentResourceCrn;

    private Set<String> slackChannelIds = new HashSet<>();

    private String distributionListManagementType;

    public CreateOrUpdateDistributionListRequestDto() {
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

    public List<EventChannelPreferenceDto> getEventChannelPreferences() {
        return eventChannelPreferences;
    }

    public void setEventChannelPreferences(List<EventChannelPreferenceDto> eventChannelPreferences) {
        this.eventChannelPreferences = eventChannelPreferences;
    }

    public Set<String> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(Set<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public String getDistributionListId() {
        return distributionListId;
    }

    public void setDistributionListId(String distributionListId) {
        this.distributionListId = distributionListId;
    }

    public String getParentResourceCrn() {
        return parentResourceCrn;
    }

    public void setParentResourceCrn(String parentResourceCrn) {
        this.parentResourceCrn = parentResourceCrn;
    }

    public Set<String> getSlackChannelIds() {
        return slackChannelIds;
    }

    public void setSlackChannelIds(Set<String> slackChannelIds) {
        this.slackChannelIds = slackChannelIds;
    }

    public String getDistributionListManagementType() {
        return distributionListManagementType;
    }

    public void setDistributionListManagementType(String distributionListManagementType) {
        this.distributionListManagementType = distributionListManagementType;
    }

    @Override
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreateOrUpdateDistributionListRequestDto that = (CreateOrUpdateDistributionListRequestDto) o;
        return Objects.equals(resourceCrn, that.resourceCrn) &&
                Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(eventChannelPreferences, that.eventChannelPreferences) &&
                Objects.equals(emailAddresses, that.emailAddresses) &&
                Objects.equals(distributionListId, that.distributionListId) &&
                Objects.equals(parentResourceCrn, that.parentResourceCrn) &&
                Objects.equals(slackChannelIds, that.slackChannelIds) &&
                Objects.equals(distributionListManagementType, that.distributionListManagementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceCrn, resourceName, eventChannelPreferences, emailAddresses,
                distributionListId, parentResourceCrn, slackChannelIds, distributionListManagementType);
    }

    @Override
    public String toString() {
        return "CreateOrUpdateDistributionListRequestDto{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", eventChannelPreferences=" + eventChannelPreferences +
                ", distributionListId='" + distributionListId + '\'' +
                ", parentResourceCrn='" + parentResourceCrn + '\'' +
                ", distributionListManagementType='" + distributionListManagementType + '\'' +
                '}';
    }
}
