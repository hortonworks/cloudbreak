package com.sequenceiq.cloudbreak.api.model.flex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexUsageComponentJson implements JsonEntity {

    private String componentId;

    private List<? extends FlexUsageComponentInstanceJson> instances;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public List<? extends FlexUsageComponentInstanceJson> getInstances() {
        return instances;
    }

    public void setInstances(List<? extends FlexUsageComponentInstanceJson> instances) {
        this.instances = instances;
    }
}
