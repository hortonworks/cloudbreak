package com.sequenceiq.periscope.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.service.configuration.AmbariConfigurationService;
import com.sequenceiq.periscope.service.configuration.ConfigParam;

public class Cluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cluster.class);
    private Map<Priority, Map<ApplicationId, SchedulerApplication>> applications;
    private boolean restarting;
    private long lastScalingActivity;
    private Configuration configuration;
    private YarnClient yarnClient;
    private ClusterMetricsInfo metrics;
    private ClusterDetails clusterDetails;

    public Cluster(ClusterDetails clusterDetails) throws ConnectionException {
        this.clusterDetails = clusterDetails;
        this.applications = new ConcurrentHashMap<>();
        initConfiguration();
    }

    public String getId() {
        return clusterDetails.getId();
    }

    public YarnClient getYarnClient() {
        return yarnClient;
    }

    public String getHost() {
        return clusterDetails.getAmbari().getHost();
    }

    public String getPort() {
        return clusterDetails.getAmbari().getPort();
    }

    public String getUser() {
        return clusterDetails.getAmbari().getUser();
    }

    public String getPass() {
        return clusterDetails.getAmbari().getPass();
    }

    public int getMinSize() {
        return clusterDetails.getMinSize();
    }

    public int getMaxSize() {
        return clusterDetails.getMaxSize();
    }

    public int getCoolDown() {
        return clusterDetails.getCoolDown();
    }

    public boolean isAppMovementAllowed() {
        return clusterDetails.isAppMovementAllowed();
    }

    public ClusterState getState() {
        return clusterDetails.getState();
    }

    public void setState(ClusterState state) {
        clusterDetails.setState(state);
    }

    public boolean isRunning() {
        return clusterDetails.getState() == ClusterState.RUNNING;
    }

    public void allowAppMovement(boolean appMovementAllowed) {
        clusterDetails.setAppMovementAllowed(appMovementAllowed);
    }

    public String getConfigValue(ConfigParam param, String defaultValue) {
        return configuration.get(param.key(), defaultValue);
    }

    public int getTotalNodes() {
        return metrics == null ? 0 : metrics.getTotalNodes();
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

    public List<Alarm> getAlarms() {
        return clusterDetails.getAlarms();
    }

    public ClusterDetails getClusterDetails() {
        return clusterDetails;
    }

    public void setClusterDetails(ClusterDetails clusterDetails) {
        this.clusterDetails = clusterDetails;
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
        LOGGER.info("Application ({}) added to cluster {}", applicationId.toString(), getId());
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
                    LOGGER.info("Application ({}) removed from cluster {}", applicationId, getId());
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
            configuration = AmbariConfigurationService.getConfiguration(newAmbariClient());
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
