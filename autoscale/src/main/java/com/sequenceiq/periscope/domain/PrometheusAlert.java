package com.sequenceiq.periscope.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.sequenceiq.periscope.api.model.AlertState;

@Entity
@DiscriminatorValue("PROMETHEUS")
@NamedQueries({
        @NamedQuery(name = "PrometheusAlert.findByCluster", query = "SELECT c FROM PrometheusAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"),
        @NamedQuery(name = "PrometheusAlert.findAllByCluster", query = "SELECT c FROM PrometheusAlert c WHERE c.cluster.id= :clusterId")
})
public class PrometheusAlert extends BaseAlert {
    @ManyToOne
    private Cluster cluster;

    @Column(name = "alert_rule")
    private String alertRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_state")
    private AlertState alertState;

    private int period;

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getAlertRule() {
        return alertRule;
    }

    public void setAlertRule(String alertRule) {
        this.alertRule = alertRule;
    }

    public AlertState getAlertState() {
        return alertState;
    }

    public void setAlertState(AlertState alertState) {
        this.alertState = alertState;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
