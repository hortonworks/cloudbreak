package com.sequenceiq.periscope.domain;

import static com.sequenceiq.periscope.common.AlertConstants.ADJUSTMENT_TYPE;
import static com.sequenceiq.periscope.common.AlertConstants.CRON;
import static com.sequenceiq.periscope.common.AlertConstants.SCALING_TARGET;
import static com.sequenceiq.periscope.common.AlertConstants.TIME_ZONE;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import com.sequenceiq.periscope.api.model.AlertType;

@Entity
@DiscriminatorValue("TIME")
@NamedQueries({
        @NamedQuery(name = "TimeAlert.findByCluster", query = "SELECT c FROM TimeAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"),
        @NamedQuery(name = "TimeAlert.findAllByCluster", query = "SELECT c FROM TimeAlert c WHERE c.cluster.id= :clusterId")
})
public class TimeAlert extends BaseAlert {

    @Column(name = "time_zone")
    private String timeZone;

    private String cron;

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public AlertType getAlertType() {
        return AlertType.TIME;
    }

    public Map<String, String> getTelemetryParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(TIME_ZONE, getTimeZone());
        parameters.put(CRON, getCron());
        parameters.put(SCALING_TARGET, "" + getScalingPolicy().getScalingAdjustment());
        parameters.put(ADJUSTMENT_TYPE, "" + getScalingPolicy().getAdjustmentType());
        return parameters;
    }
}
