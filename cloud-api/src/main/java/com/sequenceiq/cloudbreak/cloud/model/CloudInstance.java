package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudInstance extends DynamicModel {

    private String instanceId;
    private InstanceTemplate template;

    public CloudInstance(String instanceId, InstanceTemplate template) {
        this.instanceId = instanceId;
        this.template = template;
    }

    public CloudInstance(String instanceId, InstanceTemplate template, Map<String, Object> params) {
        super(params);
        this.instanceId = instanceId;
        this.template = template;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public InstanceTemplate getTemplate() {
        return template;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudInstance{");
        sb.append("instanceId='").append(instanceId).append('\'');
        sb.append(", template=").append(template);
        sb.append('}');
        return sb.toString();
    }
}
