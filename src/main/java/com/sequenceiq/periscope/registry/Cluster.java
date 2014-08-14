package com.sequenceiq.periscope.registry;

import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.model.Priority;
import com.sequenceiq.periscope.model.Queue;
import com.sequenceiq.periscope.model.QueueSetup;
import com.sequenceiq.periscope.model.SchedulerApplication;
import com.sequenceiq.periscope.policies.cloudbreak.CloudbreakPolicy;
import com.sequenceiq.periscope.service.configuration.AmbariConfigurationService;
import com.sequenceiq.periscope.service.configuration.ConfigParam;
import com.sequenceiq.periscope.utils.ClusterUtils;

public class Cluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cluster.class);
    private static final String CAPACITY_SCHEDULER = "capacity-scheduler";
    private static final String ROOT_PREFIX = "yarn.scheduler.capacity.root.";
    private static final String QUEUE_NAMES = ROOT_PREFIX + "queues";
    private static final String DEFAULT_QUEUE_NAME = "default";
    private final Map<Priority, Map<ApplicationId, SchedulerApplication>> applications;
    private final String id;
    private final Ambari ambari;
    private boolean appMovementAllowed = true;
    private Configuration configuration;
    private YarnClient yarnClient;
    private ClusterMetricsInfo metrics;
    private CloudbreakPolicy cloudbreakPolicy;
    private ClusterState state = ClusterState.RUNNING;

    public Cluster(String id, Ambari ambari) throws ConnectionException {
        this.id = id;
        this.ambari = ambari;
        this.applications = new ConcurrentHashMap<>();
        initConfiguration();
    }

    public String getId() {
        return id;
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

    public boolean isAppMovementAllowed() {
        return appMovementAllowed;
    }

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
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

    public int getTotalNodes() {
        return metrics == null ? 0 : metrics.getTotalNodes();
    }

    public long getTotalMB() {
        return metrics == null ? 0 : metrics.getTotalMB();
    }

    public CloudbreakPolicy getCloudbreakPolicy() {
        return cloudbreakPolicy;
    }

    public void setCloudbreakPolicy(CloudbreakPolicy cloudbreakPolicy) {
        this.cloudbreakPolicy = cloudbreakPolicy;
    }

    public int scale() {
        return metrics == null || cloudbreakPolicy == null ? 0 : cloudbreakPolicy.scale(metrics);
    }

    public void updateMetrics(ClusterMetricsInfo metrics) {
        this.metrics = metrics == null ? this.metrics : metrics;
    }

    public void refreshConfiguration() throws ConnectionException {
        initConfiguration();
    }

    public Map<String, String> setQueueSetup(QueueSetup queueSetup) throws QueueSetupException {
        AmbariClient ambariClient = newAmbariClient();
        Map<String, String> csConfig = ambariClient.getServiceConfigMap().get(CAPACITY_SCHEDULER);
        validateCSConfig(csConfig, queueSetup);
        Map<String, String> newConfig = generateNewQueueConfig(csConfig, queueSetup);
        ambariClient.modifyConfiguration(CAPACITY_SCHEDULER, newConfig);
        // TODO refresh queues
        return newConfig;
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
        LOGGER.info("Application ({}) added to cluster {}", applicationId.toString(), this.id);
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
                    LOGGER.info("Application ({}) removed from cluster {}", applicationId, this.id);
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
            throw new ConnectionException(ambari.getHost());
        }
    }

    private AmbariClient newAmbariClient() {
        return new AmbariClient(ambari.getHost(), ambari.getPort(), ambari.getUser(), ambari.getPass());
    }

    private void validateCSConfig(Map<String, String> csConfig, QueueSetup queueSetup) throws QueueSetupException {
        if (csConfig == null) {
            throwQueueSetupException("Capacity-scheduler config not found", csConfig);
        }
        int capacity = 0;
        List<String> queueNames = new ArrayList<>(queueSetup.getSetup().size());
        for (Queue queue : queueSetup.getSetup()) {
            String name = queue.getName();
            if (queueNames.contains(name)) {
                throwQueueSetupException("Queue name: " + name + " specified twice", csConfig);
            }
            capacity += queue.getCapacity();
            queueNames.add(name);
        }
        if (capacity != ClusterUtils.MAX_CAPACITY) {
            throwQueueSetupException("Global queue capacities must be 100", csConfig);
        }
        if (!queueNames.contains(DEFAULT_QUEUE_NAME)) {
            throwQueueSetupException("Default queue must exist", csConfig);
        }
    }

    private Map<String, String> generateNewQueueConfig(Map<String, String> csConfig, QueueSetup queueSetup) {
        Map<String, String> config = new HashMap<>();
        List<String> newQueueNames = new LinkedList<>();
        for (Queue queue : queueSetup.getSetup()) {
            String name = queue.getName();
            int capacity = queue.getCapacity();
            config.put(ROOT_PREFIX + name + ".acl_administer_jobs", "*");
            config.put(ROOT_PREFIX + name + ".acl_submit_applications", "*");
            config.put(ROOT_PREFIX + name + ".capacity", "" + capacity);
            config.put(ROOT_PREFIX + name + ".maximum-capacity", "" + capacity);
            config.put(ROOT_PREFIX + name + ".state", "RUNNING");
            config.put(ROOT_PREFIX + name + ".user-limit-factor", "1");
            newQueueNames.add(name);
        }
        copyNonQueueRelatedProperties(csConfig, config);
        config.put(QUEUE_NAMES, arrayToCommaDelimitedString(newQueueNames.toArray()));
        return config;
    }

    private void copyNonQueueRelatedProperties(Map<String, String> from, Map<String, String> to) {
        String[] queueNames = getQueueNames(from);
        for (String key : from.keySet()) {
            if (!isQueueProperty(key, queueNames)) {
                to.put(key, from.get(key));
            }
        }
    }

    private String[] getQueueNames(Map<String, String> csConfig) {
        return csConfig.get(QUEUE_NAMES).split(",");
    }

    private boolean isQueueProperty(String key, String[] queueNames) {
        boolean result = false;
        for (String name : queueNames) {
            if (key.startsWith(ROOT_PREFIX + name)) {
                result = true;
            }
        }
        return result;
    }

    private void throwQueueSetupException(String message, Map<String, String> csConfig) throws QueueSetupException {
        throw new QueueSetupException(message, csConfig);
    }

}
