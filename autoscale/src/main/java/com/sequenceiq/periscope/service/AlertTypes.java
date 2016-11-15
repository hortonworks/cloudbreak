package com.sequenceiq.periscope.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("cb")
@Component
public class AlertTypes {

    private String alerts = "HDFS Usage:hdfs_usage";

    public String getAlerts() {
        return alerts;
    }

    public void setAlerts(String alerts) {
        this.alerts = alerts;
    }
}
