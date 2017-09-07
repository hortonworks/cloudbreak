package com.sequenceiq.cloudbreak.service.events;

public class InstanceGroupEventData extends CloudbreakEventData {

    private String instanceGroupName;

    public InstanceGroupEventData(Long entityId, String eventType, String eventMessage) {
        super(entityId, eventType, eventMessage);
    }

    public InstanceGroupEventData(Long entityId, String eventType, String eventMessage, String instanceGroup) {
        super(entityId, eventType, eventMessage);
        instanceGroupName = instanceGroup;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudbreakEventData{");
        sb.append("entityId=").append(getEntityId());
        sb.append(", eventType='").append(getEventType()).append('\'');
        sb.append(", eventMessage='").append(getEventMessage()).append('\'');
        sb.append(", instanceGroup='").append(instanceGroupName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
