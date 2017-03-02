package com.sequenceiq.periscope.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@DiscriminatorValue("TIME")
@NamedQueries({
        @NamedQuery(name = "TimeAlert.findByCluster", query = "SELECT c FROM TimeAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"),
        @NamedQuery(name = "TimeAlert.findAllByCluster", query = "SELECT c FROM TimeAlert c WHERE c.cluster.id= :clusterId")
})
public class TimeAlert extends BaseAlert {

    @ManyToOne
    private Cluster cluster;

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

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }
}
