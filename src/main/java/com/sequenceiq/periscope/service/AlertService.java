package com.sequenceiq.periscope.service;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.utils.AmbariClientProvider;

import freemarker.template.Configuration;

@Service
public class AlertService {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(AlertService.class);
    private static final String ALERT_PATH = "alerts/";
    private static final String MEMORY_ALERT = "allocated_memory.ftl";
    private static final String CONTAINER_ALERT = "pending_containers.ftl";
    private static final String APP_ALERT = "pending_apps.ftl";

    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private MetricAlertRepository metricAlertRepository;
    @Autowired
    private TimeAlertRepository timeAlertRepository;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private Configuration freemarker;
    @Autowired
    private AmbariClientProvider ambariClientProvider;

    public MetricAlert createMetricAlert(long clusterId, MetricAlert alert) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        cluster.addMetricAlert(alert);
        alert.setCluster(cluster);
        alert = metricAlertRepository.save(alert);
        clusterRepository.save(cluster);
        return alert;
    }

    public MetricAlert updateMetricAlert(long clusterId, long alertId, MetricAlert metricAlert) {
        MetricAlert alert = findMetricAlertByCluster(clusterId, alertId);
        alert.setName(metricAlert.getName());
        alert.setDefinitionName(metricAlert.getDefinitionName());
        alert.setPeriod(metricAlert.getPeriod());
        alert.setDescription(metricAlert.getDescription());
        alert.setAlertState(metricAlert.getAlertState());
        return metricAlertRepository.save(alert);
    }

    public MetricAlert findMetricAlertByCluster(long clusterId, long alertId) {
        return metricAlertRepository.findByCluster(alertId, clusterId);
    }

    public void deleteMetricAlert(long clusterId, long alertId) {
        MetricAlert alert = metricAlertRepository.findByCluster(alertId, clusterId);
        metricAlertRepository.delete(alert);
    }

    public List<MetricAlert> getMetricAlerts(long clusterId) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        return cluster.getMetricAlerts();
    }

    public TimeAlert createTimeAlert(long clusterId, TimeAlert alert) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        cluster.addTimeAlert(alert);
        alert.setCluster(cluster);
        alert = timeAlertRepository.save(alert);
        clusterRepository.save(cluster);
        return alert;
    }

    public TimeAlert findTimeAlertByCluster(long clusterId, long alertId) {
        return timeAlertRepository.findByCluster(alertId, clusterId);
    }

    public TimeAlert updateTimeAlert(long clusterId, long alertId, TimeAlert timeAlert) {
        TimeAlert alert = timeAlertRepository.findByCluster(alertId, clusterId);
        alert.setDescription(timeAlert.getDescription());
        alert.setCron(timeAlert.getCron());
        alert.setTimeZone(timeAlert.getTimeZone());
        alert.setName(timeAlert.getName());
        return timeAlertRepository.save(alert);
    }

    public List<TimeAlert> getTimeAlerts(long clusterId) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        return cluster.getTimeAlerts();
    }

    public void deleteTimeAlert(long clusterId, long alertId) {
        TimeAlert alert = timeAlertRepository.findByCluster(alertId, clusterId);
        timeAlertRepository.delete(alert);
    }

    public BaseAlert getBaseAlert(long clusterId, long alertId) {
        try {
            return findMetricAlertByCluster(clusterId, alertId);
        } catch (Exception e) {
            return findTimeAlertByCluster(clusterId, alertId);
        }
    }

    public void save(BaseAlert alert) {
        if (alert instanceof MetricAlert) {
            metricAlertRepository.save((MetricAlert) alert);
        } else {
            timeAlertRepository.save((TimeAlert) alert);
        }
    }

    public List<Map<String, String>> getAlertDefinitions(long clusterId) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        return ambariClientProvider.createAmbariClient(cluster).getAlertDefinitions();
    }

    public void addPeriscopeAlerts(Cluster cluster) {
        long clusterId = cluster.getId();
        AmbariClient client = ambariClientProvider.createAmbariClient(cluster);
        try {
            createAlert(clusterId, client, getAlertDefinition(client, CONTAINER_ALERT), CONTAINER_ALERT);
            createAlert(clusterId, client, getAlertDefinition(client, APP_ALERT), APP_ALERT);
        } catch (Exception e) {
            LOGGER.error(clusterId, "Cannot parse alert definitions", e);
        }
    }

    private String getAlertDefinition(AmbariClient client, String name) throws Exception {
        Map<String, String> model = Collections.singletonMap("clusterName", client.getClusterName());
        return processTemplateIntoString(freemarker.getTemplate(ALERT_PATH + name, "UTF-8"), model);
    }

    private void createAlert(long clusterId, AmbariClient client, String json, String alertName) {
        try {
            client.createAlert(json);
            LOGGER.info(clusterId, "Alert: {} added to the cluster", alertName);
        } catch (Exception e) {
            LOGGER.info(clusterId, "Cannot add '{}' to the cluster", alertName);
        }
    }
}
