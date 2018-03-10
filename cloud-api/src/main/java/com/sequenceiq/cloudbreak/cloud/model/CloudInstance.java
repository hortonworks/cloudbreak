package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudInstance extends DynamicModel {

    public static final String DISCOVERY_NAME = "DiscoveryName";

    public static final String SUBNET_ID = "subnetId";

    private final String instanceId;

    private final InstanceTemplate template;

    private final InstanceAuthentication authentication;

    public CloudInstance(String instanceId, InstanceTemplate template, InstanceAuthentication authentication) {
        this.instanceId = instanceId;
        this.template = template;
        this.authentication = authentication;
    }

    public CloudInstance(String instanceId, InstanceTemplate template, InstanceAuthentication authentication, Map<String, Object> params) {
        super(params);
        this.instanceId = instanceId;
        this.template = template;
        this.authentication = authentication;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public InstanceTemplate getTemplate() {
        return template;
    }

    public InstanceAuthentication getAuthentication() {
        return authentication;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudInstance{");
        sb.append("instanceId='").append(instanceId).append('\'');
        sb.append(", template=").append(template);
        sb.append(", authentication=").append(authentication);
        sb.append('}');
        return sb.toString();
    }
}
