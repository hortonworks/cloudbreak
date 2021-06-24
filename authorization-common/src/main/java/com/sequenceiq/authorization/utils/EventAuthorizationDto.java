package com.sequenceiq.authorization.utils;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class EventAuthorizationDto {

    private String resourceCrn;

    private String resourceType;

    private String eventType;

    public EventAuthorizationDto(String resourceCrn, String resourceType, String eventType) {
        this.resourceType = resourceType;
        this.resourceCrn = resourceCrn;
        this.eventType = eventType;
    }

    public EventAuthorizationDto() {
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "EventAuthorizationDto{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventAuthorizationDto)) {
            return false;
        }
        EventAuthorizationDto that = (EventAuthorizationDto) o;
        return new EqualsBuilder().append(getResourceCrn(), that.getResourceCrn()).append(getResourceType(), that.getResourceType()).append(getEventType(),
                that.getEventType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getResourceCrn()).append(getResourceType()).append(getEventType()).toHashCode();
    }

}
