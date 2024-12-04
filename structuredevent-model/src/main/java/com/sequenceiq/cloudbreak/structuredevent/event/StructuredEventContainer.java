package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredEventContainer {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<StructuredFlowEvent> flow = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<StructuredRestCallEvent> rest = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<StructuredNotificationEvent> notification = new ArrayList<>();

    public StructuredEventContainer() {
    }

    public StructuredEventContainer(List<StructuredFlowEvent> flow, List<StructuredRestCallEvent> rest, List<StructuredNotificationEvent> notification) {
        this.flow = flow;
        this.rest = rest;
        this.notification = notification;
    }

    public List<StructuredFlowEvent> getFlow() {
        return flow != null ? flow : Collections.emptyList();
    }

    public void setFlow(List<StructuredFlowEvent> flow) {
        this.flow = flow;
    }

    public List<StructuredRestCallEvent> getRest() {
        return rest != null ? rest : Collections.emptyList();
    }

    public void setRest(List<StructuredRestCallEvent> rest) {
        this.rest = rest;
    }

    public List<StructuredNotificationEvent> getNotification() {
        return notification != null ? notification : Collections.emptyList();
    }

    public void setNotification(List<StructuredNotificationEvent> notification) {
        this.notification = notification;
    }
}
