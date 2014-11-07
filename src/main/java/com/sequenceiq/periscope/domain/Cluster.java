package com.sequenceiq.periscope.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.model.Priority;
import com.sequenceiq.periscope.model.SchedulerApplication;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.service.configuration.AmbariConfigurationService;
import com.sequenceiq.periscope.service.configuration.ConfigParam;

@Entity
public class Cluster {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(Cluster.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "sequence_table")
    private long id;
    @OneToOne(cascade = CascadeType.ALL)
    private Ambari ambari;
    @ManyToOne
    private PeriscopeUser user;
    private boolean appMovementAllowed;
    private ClusterState state = ClusterState.RUNNING;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(nullable = true)
    private List<MetricAlarm> metricAlarms = new ArrayList<>();
    @JoinColumn(nullable = true)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeAlarm> timeAlarms = new ArrayList<>();
    private int minSize = -1;
    private int maxSize = -1;
    private int coolDown = -1;

    @Transient
    private Map<Priority, Map<ApplicationId, SchedulerApplication>> applications;
    @Transient
    private boolean restarting;
    @Transient
    private long lastScalingActivity;
    @Transient
    private Configuration configuration;
    @Transient
    private YarnClient yarnClient;
    @Transient
    private ClusterMetricsInfo metrics;

    public Cluster() {
    }

    public Cluster(PeriscopeUser user, Ambari ambari) throws ConnectionException {
        this.user = user;
        this.ambari = ambari;
        this.applications = new ConcurrentHashMap<>();
        initConfiguration();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isAppMovementAllowed() {
        return appMovementAllowed;
    }

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }

    public void deleteAlarms() {
        setMetricAlarms(null);
        setTimeAlarms(null);
    }

    public List<BaseAlarm> getAlarms() {
        List<BaseAlarm> alarms = new ArrayList<>();
        alarms.addAll(timeAlarms);
        alarms.addAll(metricAlarms);
        return alarms;
    }

    public void setAlarms(List<BaseAlarm> alarms) {
        List<TimeAlarm> timeAlarms = new ArrayList<>();
        List<MetricAlarm> metricAlarms = new ArrayList<>();
        for (BaseAlarm alarm : alarms) {
            if (alarm instanceof TimeAlarm) {
                timeAlarms.add((TimeAlarm) alarm);
            } else {
                metricAlarms.add((MetricAlarm) alarm);
            }
        }
        setTimeAlarms(timeAlarms);
        setMetricAlarms(metricAlarms);
    }

    public List<MetricAlarm> getMetricAlarms() {
        return metricAlarms;
    }

    public void setMetricAlarms(List<MetricAlarm> metricAlarms) {
        this.metricAlarms = metricAlarms;
    }

    public List<TimeAlarm> getTimeAlarms() {
        return timeAlarms;
    }

    public void setTimeAlarms(List<TimeAlarm> timeAlarms) {
        this.timeAlarms = timeAlarms;
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

    public YarnClient getYarnClient() {
        return yarnClient;
    }

    public String getHost() {
        return ambari.getHost();
    }

    public String getPort() {
        return ambari.getPort();
    }

    public String getUser() {
        return ambari.getUser();
    }

    public String getPass() {
        return ambari.getPass();
    }

    public boolean isRunning() {
        return state == ClusterState.RUNNING;
    }

    public void allowAppMovement(boolean appMovementAllowed) {
        this.appMovementAllowed = appMovementAllowed;
    }

    public String getConfigValue(ConfigParam param, String defaultValue) {
        return configuration.get(param.key(), defaultValue);
    }

    public long getTotalMB() {
        return metrics == null ? 0 : metrics.getTotalMB();
    }

    public boolean isRestarting() {
        return restarting;
    }

    public void setRestarting(boolean restarting) {
        this.restarting = restarting;
    }

    public long getLastScalingActivity() {
        return lastScalingActivity;
    }

    public void setLastScalingActivityCurrent() {
        this.lastScalingActivity = System.currentTimeMillis();
    }

    public void updateMetrics(ClusterMetricsInfo metrics) {
        this.restarting = false;
        this.metrics = metrics == null ? this.metrics : metrics;
    }

    public void refreshConfiguration() throws ConnectionException {
        initConfiguration();
    }

    public synchronized SchedulerApplication addApplication(ApplicationReport appReport) {
        return addApplication(appReport, Priority.NORMAL);
    }

    public synchronized SchedulerApplication addApplication(ApplicationReport appReport, Priority priority) {
        SchedulerApplication application = new SchedulerApplication(appReport, priority);
        return addApplication(application, priority);
    }

    public synchronized SchedulerApplication addApplication(SchedulerApplication application, Priority priority) {
        Map<ApplicationId, SchedulerApplication> applicationMap = applications.get(priority);
        if (applicationMap == null) {
            applicationMap = new TreeMap<>();
            applications.put(priority, applicationMap);
        }
        ApplicationId applicationId = application.getApplicationId();
        applicationMap.put(applicationId, application);
        LOGGER.info(this.id, "Application '{}' added", applicationId.toString());
        return application;
    }

    public synchronized SchedulerApplication removeApplication(ApplicationId applicationId) {
        for (Priority priority : applications.keySet()) {
            Map<ApplicationId, SchedulerApplication> apps = applications.get(priority);
            Iterator<ApplicationId> iterator = apps.keySet().iterator();
            while (iterator.hasNext()) {
                ApplicationId id = iterator.next();
                if (id.equals(applicationId)) {
                    SchedulerApplication application = apps.get(id);
                    iterator.remove();
                    LOGGER.info(this.id, "Application '{}' removed", applicationId);
                    return application;
                }
            }
        }
        return null;
    }

    public synchronized SchedulerApplication setApplicationPriority(ApplicationId applicationId, Priority newPriority) {
        SchedulerApplication application = removeApplication(applicationId);
        if (application != null) {
            application.setPriority(newPriority);
            addApplication(application, newPriority);
        }
        return application;
    }

    public Map<ApplicationId, SchedulerApplication> getApplications(Priority priority) {
        Map<ApplicationId, SchedulerApplication> copy = new TreeMap<>();
        Map<ApplicationId, SchedulerApplication> apps = applications.get(priority);
        if (apps != null) {
            copy.putAll(apps);
        }
        return copy;
    }

    public Map<Priority, Map<ApplicationId, SchedulerApplication>> getApplicationsPriorityOrder() {
        Map<Priority, Map<ApplicationId, SchedulerApplication>> copy = new TreeMap<>();
        for (Priority priority : applications.keySet()) {
            copy.put(priority, new TreeMap<>(applications.get(priority)));
        }
        return copy;
    }

    public SchedulerApplication getApplication(ApplicationId applicationId) {
        for (Priority priority : applications.keySet()) {
            Map<ApplicationId, SchedulerApplication> apps = applications.get(priority);
            for (ApplicationId id : apps.keySet()) {
                if (applicationId.equals(id)) {
                    return apps.get(id);
                }
            }
        }
        return null;
    }

    public AmbariClient newAmbariClient() {
        return new AmbariClient(getHost(), getPort(), getUser(), getPass());
    }

    private void initConfiguration() throws ConnectionException {
        try {
            configuration = AmbariConfigurationService.getConfiguration(id, newAmbariClient());
            if (yarnClient != null) {
                yarnClient.stop();
            }
            yarnClient = YarnClient.createYarnClient();
            yarnClient.init(configuration);
            yarnClient.start();
        } catch (Exception e) {
            throw new ConnectionException(getHost());
        }
    }

}
