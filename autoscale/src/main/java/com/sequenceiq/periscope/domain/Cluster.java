package com.sequenceiq.periscope.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.monitor.Monitored;

@Entity
public class Cluster implements Monitored, Clustered {

    private static final int DEFAULT_MIN_SIZE = 2;

    private static final int DEFAULT_MAX_SIZE = 100;

    private static final int DEFAULT_COOLDOWN = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "cluster_id_seq", allocationSize = 1)
    private long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ClusterPertain clusterPertain;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Ambari ambari;

    @OneToOne(mappedBy = "cluster", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private SecurityConfig securityConfig;

    @Enumerated(EnumType.STRING)
    private ClusterState state = ClusterState.PENDING;

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<MetricAlert> metricAlerts = new HashSet<>();

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<TimeAlert> timeAlerts = new HashSet<>();

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<PrometheusAlert> prometheusAlerts = new HashSet<>();

    @Column(name = "min_size")
    private int minSize = DEFAULT_MIN_SIZE;

    @Column(name = "max_size")
    private int maxSize = DEFAULT_MAX_SIZE;

    @Column(name = "cooldown")
    private int coolDown = DEFAULT_COOLDOWN;

    @Column(name = "cb_stack_id")
    private Long stackId;

    @Column(name = "last_scaling_activity")
    private volatile long lastScalingActivity;

    @Column(name = "autoscaling_enabled")
    private boolean autoscalingEnabled;

    private String periscopeNodeId;

    @Column(name = "lastevaulated")
    private long lastEvaluated;

    public Cluster() {
    }

    public Cluster(AmbariStack ambariStack) {
        stackId = ambariStack.getStackId();
        ambari = ambariStack.getAmbari();
    }

    public void update(AmbariStack ambariStack) {
        Ambari ambari = ambariStack.getAmbari();
        this.ambari.setHost(ambari.getHost());
        this.ambari.setPort(ambari.getPort());
        this.ambari.setUser(ambari.getUser());
        this.ambari.setPass(ambari.getPass());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ClusterPertain getClusterPertain() {
        return clusterPertain;
    }

    public void setClusterPertain(ClusterPertain clusterPertain) {
        this.clusterPertain = clusterPertain;
    }

    public Ambari getAmbari() {
        return ambari;
    }

    public void setAmbari(Ambari ambari) {
        this.ambari = ambari;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }

    public Set<MetricAlert> getMetricAlerts() {
        return metricAlerts;
    }

    public void setMetricAlerts(Set<MetricAlert> metricAlerts) {
        this.metricAlerts = metricAlerts;
    }

    public Set<TimeAlert> getTimeAlerts() {
        return timeAlerts;
    }

    public void setTimeAlerts(Set<TimeAlert> timeAlerts) {
        this.timeAlerts = timeAlerts;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public long getLastScalingActivity() {
        return lastScalingActivity;
    }

    public void setLastScalingActivity(long lastScalingActivity) {
        this.lastScalingActivity = lastScalingActivity;
    }

    public String getHost() {
        return ambari.getHost();
    }

    public String getPort() {
        return ambari.getPort();
    }

    public String getAmbariUser() {
        return ambari.getUser();
    }

    public String getAmbariPass() {
        return ambari.getPass();
    }

    public synchronized void setLastScalingActivityCurrent() {
        lastScalingActivity = System.currentTimeMillis();
    }

    public boolean isRunning() {
        return ClusterState.RUNNING == state;
    }

    public void addMetricAlert(MetricAlert alert) {
        metricAlerts.add(alert);
    }

    public void addTimeAlert(TimeAlert alert) {
        timeAlerts.add(alert);
    }

    public Set<PrometheusAlert> getPrometheusAlerts() {
        return prometheusAlerts;
    }

    public void setPrometheusAlerts(Set<PrometheusAlert> prometheusAlerts) {
        this.prometheusAlerts = prometheusAlerts;
    }

    public void addPrometheusAlert(PrometheusAlert alert) {
        prometheusAlerts.add(alert);
    }

    public boolean isAutoscalingEnabled() {
        return autoscalingEnabled;
    }

    public void setAutoscalingEnabled(boolean autoscalingEnabled) {
        this.autoscalingEnabled = autoscalingEnabled;
    }

    public String getPeriscopeNodeId() {
        return periscopeNodeId;
    }

    public void setPeriscopeNodeId(String periscopeNodeId) {
        this.periscopeNodeId = periscopeNodeId;
    }

    public long getLastEvaluated() {
        return lastEvaluated;
    }

    @Override
    public void setLastEvaluated(long lastEvaluated) {
        this.lastEvaluated = lastEvaluated;
    }

    @Override
    public Cluster getCluster() {
        return this;
    }
}


