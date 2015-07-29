package com.sequenceiq.cloudbreak.cloud.model;

public class CloudInstance {

    private String instanceId;

    private CloudInstanceMetaData metaData;

    private InstanceTemplate template;

    public CloudInstance(String instanceId, CloudInstanceMetaData metaData, InstanceTemplate template) {
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

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudInstance{" +
                "instanceId='" + instanceId + '\'' +
                ", metaData=" + metaData +
                ", template=" + template +
                '}';
    }

    //END GENERATED CODE
}
