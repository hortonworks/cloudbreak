package com.sequenceiq.periscope.rest.json;

import java.util.List;

public abstract class AbstractAlarmJson implements Json {

    private Long id;
    private String alarmName;
    private String description;
    private Long scalingPolicyId;
    private List<NotificationJson> notifications;

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScalingPolicyId() {
        return scalingPolicyId;
    }

    public void setScalingPolicyId(Long scalingPolicyId) {
        this.scalingPolicyId = scalingPolicyId;
    }

    public List<NotificationJson> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationJson> notifications) {
        this.notifications = notifications;
    }
}
