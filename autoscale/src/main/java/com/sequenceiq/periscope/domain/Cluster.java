package com.sequenceiq.periscope.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.converter.TunnelConverter;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.converter.db.StackTypeAttributeConverter;
import com.sequenceiq.periscope.model.MonitoredStack;
import com.sequenceiq.periscope.monitor.Monitored;
import com.sequenceiq.periscope.monitor.evaluator.ScalingConstants;

@Entity
public class Cluster implements Monitored, Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "cluster_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne
    private ClusterPertain clusterPertain;

    @JoinColumn(name = "cluster_manager_id")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ClusterManager clusterManager;

    @OneToOne(mappedBy = "cluster", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private SecurityConfig securityConfig;

    @Enumerated(EnumType.STRING)
    private ClusterState state = ClusterState.PENDING;

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<TimeAlert> timeAlerts = new HashSet<>();

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<LoadAlert> loadAlerts = new HashSet<>();

    @Column(name = "min_size")
    private Integer minSize = ScalingConstants.DEFAULT_CLUSTER_MIN_SIZE;

    @Column(name = "max_size")
    private Integer maxSize = ScalingConstants.DEFAULT_CLUSTER_MAX_SIZE;

    @Column(name = "cooldown")
    private Integer coolDown = ScalingConstants.DEFAULT_CLUSTER_COOLDOWN_MINS;

    @Column(name = "stop_start_enabled")
    private Boolean stopStartScalingEnabled = ScalingConstants.STOP_START_SCALING_ENABLED;

    @Column(name = "cb_stack_crn")
    private String stackCrn;

    @Column(name = "cb_stack_name")
    private String stackName;

    @Column(name = "environment_crn")
    private String environmentCrn;

    @Column(name = "machine_user_crn")
    private String machineUserCrn;

    @Column(name = "cloud_platform")
    private String cloudPlatform;

    @Column(name = "cb_stack_type")
    @Convert(converter = StackTypeAttributeConverter.class)
    private StackType stackType = StackType.TEMPLATE;

    @Column(name = "last_scaling_activity")
    private volatile long lastScalingActivity;

    @Column(name = "autoscaling_enabled")
    private Boolean autoscalingEnabled = false;

    private String periscopeNodeId;

    @Column(name = "lastevaulated")
    private long lastEvaluated;

    @Column(name = "cb_stack_id")
    private long stackId;

    @Convert(converter = TunnelConverter.class)
    private Tunnel tunnel = Tunnel.DIRECT;

    public Cluster() {
    }

    public Cluster(Long clusterId) {
        this.id = clusterId;
    }

    public Cluster(MonitoredStack monitoredStack) {
        setStackCrn(monitoredStack.getStackCrn());
        clusterManager = monitoredStack.getClusterManager();
    }

    public void update(MonitoredStack monitoredStack) {
        ClusterManager clusterManager = monitoredStack.getClusterManager();
        this.clusterManager.setHost(clusterManager.getHost());
        this.clusterManager.setPort(clusterManager.getPort());
        this.clusterManager.setUser(clusterManager.getUser());
        this.clusterManager.setPass(clusterManager.getPass());
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public long getStackId() {
        return stackId;
    }

    public void setStackId(long stackId) {
        this.stackId = stackId;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
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

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
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

    public Set<TimeAlert> getTimeAlerts() {
        return timeAlerts;
    }

    public void setTimeAlerts(Set<TimeAlert> timeAlerts) {
        this.timeAlerts = timeAlerts;
    }

    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public Integer getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(Integer coolDown) {
        this.coolDown = coolDown;
    }

    public long getLastScalingActivity() {
        return lastScalingActivity;
    }

    public void setLastScalingActivity(long lastScalingActivity) {
        this.lastScalingActivity = lastScalingActivity;
    }

    public String getHost() {
        return clusterManager.getHost();
    }

    public String getPort() {
        return clusterManager.getPort();
    }

    public String getClusterManagerUser() {
        return clusterManager.getUser();
    }

    public String getClusterManagerPassword() {
        return clusterManager.getPass();
    }

    public synchronized void setLastScalingActivityCurrent() {
        lastScalingActivity = System.currentTimeMillis();
    }

    public boolean isRunning() {
        return ClusterState.RUNNING == state;
    }

    public void addTimeAlert(TimeAlert alert) {
        timeAlerts.add(alert);
    }

    public void addLoadAlert(LoadAlert alert) {
        loadAlerts.add(alert);
    }

    public Boolean isAutoscalingEnabled() {
        return autoscalingEnabled;
    }

    public void setAutoscalingEnabled(Boolean autoscalingEnabled) {
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

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public StackType getStackType() {
        return stackType;
    }

    public void setStackType(StackType stackType) {
        this.stackType = stackType;
    }

    @Override
    public void setLastEvaluated(long lastEvaluated) {
        this.lastEvaluated = lastEvaluated;
    }

    @Override
    public Cluster getCluster() {
        return this;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public Set<LoadAlert> getLoadAlerts() {
        return loadAlerts;
    }

    public void setLoadAlerts(Set<LoadAlert> loadAlerts) {
        this.loadAlerts = loadAlerts;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Boolean getAutoscalingEnabled() {
        return autoscalingEnabled;
    }

    public Boolean isStopStartScalingEnabled() {
        return stopStartScalingEnabled;
    }

    public void setStopStartScalingEnabled(Boolean stopStartScalingEnabled) {
        this.stopStartScalingEnabled = stopStartScalingEnabled;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getMachineUserCrn() {
        return machineUserCrn;
    }

    public void setMachineUserCrn(String machineUserCrn) {
        this.machineUserCrn = machineUserCrn;
    }
}


