package com.sequenceiq.periscope.api.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.annotations.ApiModelProperty;

public class ClusterBaseJson implements Json {

    @ApiModelProperty(ClusterJsonsProperties.HOST)
    private String host;

    @ApiModelProperty(ClusterJsonsProperties.PORT)
    private String port;

    @ApiModelProperty(ClusterJsonsProperties.USERNAME)
    private String user;

    @ApiModelProperty(ClusterJsonsProperties.STACK_ID)
    @NotNull
    private Long stackId;

    @ApiModelProperty(ClusterJsonsProperties.METRIC_ALERTS)
    private List<MetricAlertJson> metricAlerts;

    @ApiModelProperty(ClusterJsonsProperties.TIME_ALERTS)
    private List<TimeAlertJson> timeAlerts;

    @ApiModelProperty(ClusterJsonsProperties.PROMETHEUS_ALERTS)
    private List<PrometheusAlertJson> prometheusAlerts;

    @ApiModelProperty(ClusterJsonsProperties.SCALING_CONFIGURATION)
    private ScalingConfigurationJson scalingConfiguration;

    public ClusterBaseJson() {
    }

    public ClusterBaseJson(String host, String port, String user, Long stackId) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.stackId = stackId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public List<MetricAlertJson> getMetricAlerts() {
        return metricAlerts;
    }

    public void setMetricAlerts(List<MetricAlertJson> metricAlerts) {
        this.metricAlerts = metricAlerts;
    }

    public List<TimeAlertJson> getTimeAlerts() {
        return timeAlerts;
    }

    public void setTimeAlerts(List<TimeAlertJson> timeAlerts) {
        this.timeAlerts = timeAlerts;
    }

    public List<PrometheusAlertJson> getPrometheusAlerts() {
        return prometheusAlerts;
    }

    public void setPrometheusAlerts(List<PrometheusAlertJson> prometheusAlerts) {
        this.prometheusAlerts = prometheusAlerts;
    }

    public ScalingConfigurationJson getScalingConfiguration() {
        return scalingConfiguration;
    }

    public void setScalingConfiguration(ScalingConfigurationJson scalingConfiguration) {
        this.scalingConfiguration = scalingConfiguration;
    }
}
