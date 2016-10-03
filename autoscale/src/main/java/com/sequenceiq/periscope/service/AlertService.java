package com.sequenceiq.periscope.service;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.utils.AmbariClientProvider;

import freemarker.template.Configuration;

@Service
public class AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertService.class);
    private static final String ALERT_PATH = "alerts/";
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
    private Configuration freemarkerConfiguration;
    @Autowired
    private AmbariClientProvider ambariClientProvider;

    public MetricAlert createMetricAlert(long clusterId, MetricAlert alert) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        alert.setCluster(cluster);
        MetricAlert metricAlert = metricAlertRepository.save(alert);
        cluster.addMetricAlert(metricAlert);
        clusterRepository.save(cluster);
        return metricAlert;
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
        metricAlertRepository.findByCluster(alertId, clusterId);
        Cluster cluster = clusterRepository.find(clusterId);
        cluster.setMetricAlerts(removeMetricAlert(cluster, alertId));
        metricAlertRepository.delete(alertId);
        clusterRepository.save(cluster);
    }

    public Set<MetricAlert> removeMetricAlert(Cluster cluster, long alertId) {
        return cluster.getMetricAlerts().stream().filter(a -> a.getId() != alertId).collect(Collectors.toSet());
    }

    public Set<MetricAlert> getMetricAlerts(long clusterId) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        return cluster.getMetricAlerts();
    }

    public TimeAlert createTimeAlert(long clusterId, TimeAlert alert) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        alert.setCluster(cluster);
        alert = timeAlertRepository.save(alert);
        cluster.addTimeAlert(alert);
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

    public Set<TimeAlert> getTimeAlerts(long clusterId) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        return cluster.getTimeAlerts();
    }

    public void deleteTimeAlert(long clusterId, long alertId) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        timeAlertRepository.findByCluster(alertId, clusterId);
        cluster.setTimeAlerts(removeTimeAlert(cluster, alertId));
        timeAlertRepository.delete(alertId);
        clusterRepository.save(cluster);
    }

    public Set<TimeAlert> removeTimeAlert(Cluster cluster, long alertId) {
        return cluster.getTimeAlerts().stream().filter(a -> a.getId() != alertId).collect(Collectors.toSet());
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

    public List<Map<String, Object>> getAlertDefinitions(long clusterId) {
        Cluster cluster = clusterService.findOneByUser(clusterId);
        List<Map<String, Object>> ret = new ArrayList<>();
        List<Map<String, String>> alertDefinitions = ambariClientProvider.createAmbariClient(cluster).getAlertDefinitions();
        for (Map<String, String> alertDefinition : alertDefinitions) {
            Map<String, Object> tmp = new HashMap<>();
            for (Map.Entry<String, String> stringStringEntry : alertDefinition.entrySet()) {
                tmp.put(stringStringEntry.getKey(), stringStringEntry.getValue());
            }
            ret.add(tmp);
        }
        return ret;
    }

    public void addPeriscopeAlerts(Cluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        AmbariClient client = ambariClientProvider.createAmbariClient(cluster);
        try {
            createAlert(client, getAlertDefinition(client, CONTAINER_ALERT), CONTAINER_ALERT);
            createAlert(client, getAlertDefinition(client, APP_ALERT), APP_ALERT);
        } catch (Exception e) {
            LOGGER.error("Cannot parse alert definitions", e);
        }
    }

    private String getAlertDefinition(AmbariClient client, String name) throws Exception {
        Map<String, String> model = Collections.singletonMap("clusterName", client.getClusterName());
        return processTemplateIntoString(freemarkerConfiguration.getTemplate(ALERT_PATH + name, "UTF-8"), model);
    }

    private void createAlert(AmbariClient client, String json, String alertName) {
        try {
            client.createAlert(json);
            LOGGER.info("Alert: {} added to the cluster", alertName);
        } catch (Exception e) {
            LOGGER.info("Cannot add '{}' to the cluster", alertName);
        }
    }
}
