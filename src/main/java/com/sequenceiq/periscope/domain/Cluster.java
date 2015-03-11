package com.sequenceiq.periscope.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.model.AmbariStack;

@Entity
@NamedQueries({
        @NamedQuery(name = "Cluster.findAllByUser", query = "SELECT c FROM Cluster c WHERE c.user.id= :id"),
        @NamedQuery(name = "Cluster.find", query = "SELECT c FROM Cluster c WHERE c.id= :id"),
        @NamedQuery(name = "Cluster.findAllByState", query = "SELECT c FROM Cluster c WHERE c.state= :state")
})
public class Cluster {

    private static final int DEFAULT_MIN_SIZE = 3;
    private static final int DEFAULT_MAX_SIZE = 100;
    private static final int DEFAULT_COOLDOWN = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "sequence_table")
    private long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Ambari ambari;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private PeriscopeUser user;

    @Enumerated(EnumType.STRING)
    private ClusterState state = ClusterState.RUNNING;

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetricAlert> metricAlerts = new ArrayList<>();

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeAlert> timeAlerts = new ArrayList<>();

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

    public Cluster() {
    }

    public Cluster(PeriscopeUser user, AmbariStack ambariStack) {
        this.user = user;
        this.stackId = ambariStack.getStackId();
        this.ambari = ambariStack.getAmbari();
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

    public Ambari getAmbari() {
        return ambari;
    }

    public void setAmbari(Ambari ambari) {
        this.ambari = ambari;
    }

    public PeriscopeUser getUser() {
        return user;
    }

    public void setUser(PeriscopeUser user) {
        this.user = user;
    }

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }

    public List<MetricAlert> getMetricAlerts() {
        return metricAlerts;
    }

    public void setMetricAlerts(List<MetricAlert> metricAlerts) {
        this.metricAlerts = metricAlerts;
    }

    public List<TimeAlert> getTimeAlerts() {
        return timeAlerts;
    }

    public void setTimeAlerts(List<TimeAlert> timeAlerts) {
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

    public AmbariClient newAmbariClient() {
        return new AmbariClient(getHost(), getPort(), getAmbariUser(), getAmbariPass());
    }

    public synchronized void setLastScalingActivityCurrent() {
        this.lastScalingActivity = System.currentTimeMillis();
    }

    public boolean isRunning() {
        return ClusterState.RUNNING == getState();
    }

    public void addMetricAlert(MetricAlert alert) {
        this.metricAlerts.add(alert);
    }

    public void addTimeAlert(TimeAlert alert) {
        this.timeAlerts.add(alert);
    }
}


