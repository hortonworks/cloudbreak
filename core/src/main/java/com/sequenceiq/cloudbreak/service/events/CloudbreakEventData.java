package com.sequenceiq.cloudbreak.service.events;

public class CloudbreakEventData {
    private Long entityId;
    private String eventType;
    private String eventMessage;

    public CloudbreakEventData(Long entityId, String eventType, String eventMessage) {
        this.entityId = entityId;
        this.eventType = eventType;
        this.eventMessage = eventMessage;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakEventData{");
        sb.append("entityId=").append(entityId);
        sb.append(", eventType='").append(eventType).append('\'');
        sb.append(", eventMessage='").append(eventMessage).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
