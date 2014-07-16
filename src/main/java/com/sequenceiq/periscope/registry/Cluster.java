package com.sequenceiq.periscope.registry;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.model.Priority;
import com.sequenceiq.periscope.model.SchedulerApplication;
import com.sequenceiq.periscope.service.configuration.AmbariConfigurationService;
import com.sequenceiq.periscope.service.configuration.ConfigParam;

public class Cluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cluster.class);
    private final String clusterId;
    private final Ambari ambari;
    private final Configuration configuration;
    private final YarnClient yarnClient;
    private final Map<Priority, Map<ApplicationId, SchedulerApplication>> applications;

    public Cluster(String clusterId, Ambari ambari) throws ConnectionException {
        this.clusterId = clusterId;
        this.ambari = ambari;
        try {
            this.applications = new ConcurrentHashMap<>();
            this.configuration = AmbariConfigurationService.getConfiguration(
                    new AmbariClient(ambari.getHost(), ambari.getPort(), ambari.getUser(), ambari.getPass()));
            this.yarnClient = YarnClient.createYarnClient();
            this.yarnClient.init(configuration);
            this.yarnClient.start();
        } catch (Exception e) {
            throw new ConnectionException(ambari.getHost());
        }
    }

    public String getClusterId() {
        return clusterId;
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

    public String getConfigValue(ConfigParam param, String defaultValue) {
        return configuration.get(param.key(), defaultValue);
    }

    public synchronized SchedulerApplication addApplication(String applicationId, Priority priority) {
        return addApplication(ConverterUtils.toApplicationId(applicationId), priority);
    }

    public synchronized SchedulerApplication addApplication(ApplicationId applicationId, Priority priority) {
        Map<ApplicationId, SchedulerApplication> applicationMap = applications.get(priority);
        if (applicationMap == null) {
            applicationMap = new TreeMap<>();
            applications.put(priority, applicationMap);
        }
        SchedulerApplication application = new SchedulerApplication(applicationId, priority);
        applicationMap.put(applicationId, application);
        LOGGER.info("Application ({}) added to cluster {}", applicationId.toString(), clusterId);
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
                    LOGGER.info("Application ({}) removed from cluster {}", applicationId, clusterId);
                    return application;
                }
            }
        }
        return null;
    }

    public synchronized SchedulerApplication setPriority(ApplicationId applicationId, Priority newPriority) {
        SchedulerApplication application = removeApplication(applicationId);
        if (application != null) {
            application.setPriority(newPriority);
            addApplication(applicationId, newPriority);
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

}
