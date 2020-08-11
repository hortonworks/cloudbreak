package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.json.AnonymizingBase64Serializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;

@JsonIgnoreProperties
public class CDPStructuredNotificationDetails implements Serializable {
    private ResourceEvent resourceEvent;

    private String resourceCrn;

    private String resourceType;

    @JsonSerialize(using = AnonymizingBase64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String payload;

    public CDPStructuredNotificationDetails(ResourceEvent resourceEvent, String resourceCrn, String resourceType, String payload) {
        this.resourceEvent = resourceEvent;
        this.resourceCrn = resourceCrn;
        this.resourceType = resourceType;
        this.payload = payload;
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ResourceEvent getResourceEvent() {
        return resourceEvent;
    }

    public void setResourceEvent(ResourceEvent resourceEvent) {
        this.resourceEvent = resourceEvent;
    }
}
