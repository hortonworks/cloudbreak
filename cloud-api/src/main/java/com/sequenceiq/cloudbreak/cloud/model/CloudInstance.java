package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudInstance extends DynamicModel {

    private String instanceId;
    private CloudInstanceMetaData metaData;
    private InstanceTemplate template;

    public CloudInstance(String instanceId, CloudInstanceMetaData metaData, InstanceTemplate template) {
        this.instanceId = instanceId;
        this.metaData = metaData;
        this.template = template;
    }

    public CloudInstance(String instanceId, CloudInstanceMetaData metaData, InstanceTemplate template, Map<String, Object> params) {
        super(params);
        this.instanceId = instanceId;
        this.metaData = metaData;
        this.template = template;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public CloudInstanceMetaData getMetaData() {
        return metaData;
    }

    public InstanceTemplate getTemplate() {
        return template;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudInstance{");
        sb.append("instanceId='").append(instanceId).append('\'');
        sb.append(", metaData=").append(metaData);
        sb.append(", template=").append(template);
        sb.append('}');
        return sb.toString();
    }
}
