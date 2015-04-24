package com.sequenceiq.cloudbreak.service.events;

public class InstanceGroupEventData extends CloudbreakEventData {

    private String instanceGroupName;

    public InstanceGroupEventData(Long entityId, String eventType, String eventMessage) {
        super(entityId, eventType, eventMessage);
    }

    public InstanceGroupEventData(Long entityId, String eventType, String eventMessage, String instanceGroup) {
        super(entityId, eventType, eventMessage);
        this.instanceGroupName = instanceGroup;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakEventData{");
        sb.append("entityId=").append(super.getEntityId());
        sb.append(", eventType='").append(super.getEventType()).append('\'');
        sb.append(", eventMessage='").append(super.getEventMessage()).append('\'');
        sb.append(", instanceGroup='").append(instanceGroupName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
