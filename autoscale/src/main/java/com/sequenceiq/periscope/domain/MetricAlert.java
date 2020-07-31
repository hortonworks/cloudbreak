package com.sequenceiq.periscope.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.sequenceiq.periscope.api.model.AlertState;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.converter.AlertStateConverter;

@Entity
@DiscriminatorValue("METRIC")
@NamedQueries({
        @NamedQuery(name = "MetricAlert.findByCluster", query = "SELECT c FROM MetricAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"),
        @NamedQuery(name = "MetricAlert.findAllByCluster", query = "SELECT c FROM MetricAlert c WHERE c.cluster.id= :clusterId")
})
public class MetricAlert extends BaseAlert {

    @Column(name = "definition_name")
    private String definitionName;

    private int period;

    @Convert(converter = AlertStateConverter.class)
    @Column(name = "alert_state")
    private AlertState alertState;

    public String getDefinitionName() {
        return definitionName;
    }

    public void setDefinitionName(String definitionName) {
        this.definitionName = definitionName;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public AlertState getAlertState() {
        return alertState;
    }

    public void setAlertState(AlertState alertState) {
        this.alertState = alertState;
    }

    public AlertType getAlertType() {
        return AlertType.METRIC;
    }
}