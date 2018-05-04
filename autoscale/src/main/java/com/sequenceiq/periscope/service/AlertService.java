package com.sequenceiq.periscope.service;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.services.CommonService;
import com.sequenceiq.periscope.api.model.AlertRuleDefinitionEntry;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.repository.PrometheusAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;

import freemarker.template.Configuration;

@Service
public class AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertService.class);

    private static final String ALERT_PATH = "alerts/";

    private static final String CONTAINER_ALERT = "pending_containers.ftl";

    private static final String APP_ALERT = "pending_apps.ftl";

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private MetricAlertRepository metricAlertRepository;

    @Inject
    private TimeAlertRepository timeAlertRepository;

    @Inject
    private PrometheusAlertRepository prometheusAlertRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private ConsulKeyValueService consulKeyValueService;

    @Inject
    private PrometheusAlertTemplateService prometheusAlertService;

    @Inject
    private ScalingService scalingPolicyService;

    public MetricAlert createMetricAlert(Long clusterId, MetricAlert alert) {
        Cluster cluster = clusterService.findOneById(clusterId);
        alert.setCluster(cluster);
        MetricAlert metricAlert = (MetricAlert) save(alert);
        cluster.addMetricAlert(metricAlert);
        clusterRepository.save(cluster);
        return metricAlert;
    }

    public MetricAlert updateMetricAlert(Long clusterId, Long alertId, MetricAlert metricAlert) {
        MetricAlert alert = findMetricAlertByCluster(clusterId, alertId);
        alert.setName(metricAlert.getName());
        alert.setDefinitionName(metricAlert.getDefinitionName());
        alert.setPeriod(metricAlert.getPeriod());
        alert.setDescription(metricAlert.getDescription());
        alert.setAlertState(metricAlert.getAlertState());
        return metricAlertRepository.save(alert);
    }

    public MetricAlert findMetricAlertByCluster(Long clusterId, Long alertId) {
        return metricAlertRepository.findByCluster(alertId, clusterId);
    }

    public void deleteMetricAlert(Long clusterId, Long alertId) {
        metricAlertRepository.findByCluster(alertId, clusterId);
        Cluster cluster = clusterRepository.findById(clusterId);
        cluster.setMetricAlerts(removeMetricAlert(cluster, alertId));
        metricAlertRepository.delete(alertId);
        clusterRepository.save(cluster);
    }

    public Set<MetricAlert> removeMetricAlert(Cluster cluster, Long alertId) {
        return cluster.getMetricAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
    }

    public Set<MetricAlert> getMetricAlerts(Long clusterId) {
        Cluster cluster = clusterService.findOneById(clusterId);
        return cluster.getMetricAlerts();
    }

    public TimeAlert createTimeAlert(Long clusterId, TimeAlert alert) {
        Cluster cluster = clusterService.findOneById(clusterId);
        alert.setCluster(cluster);
        alert = (TimeAlert) save(alert);
        cluster.addTimeAlert(alert);
        clusterRepository.save(cluster);
        return alert;
    }

    public TimeAlert findTimeAlertByCluster(Long clusterId, Long alertId) {
        return timeAlertRepository.findByCluster(alertId, clusterId);
    }

    public TimeAlert updateTimeAlert(Long clusterId, Long alertId, TimeAlert timeAlert) {
        TimeAlert alert = timeAlertRepository.findByCluster(alertId, clusterId);
        alert.setDescription(timeAlert.getDescription());
        alert.setCron(timeAlert.getCron());
        alert.setTimeZone(timeAlert.getTimeZone());
        alert.setName(timeAlert.getName());
        return timeAlertRepository.save(alert);
    }

    public Set<TimeAlert> getTimeAlerts(Long clusterId) {
        Cluster cluster = clusterService.findOneById(clusterId);
        return cluster.getTimeAlerts();
    }

    public void deleteTimeAlert(Long clusterId, Long alertId) {
        Cluster cluster = clusterService.findOneById(clusterId);
        timeAlertRepository.findByCluster(alertId, clusterId);
        cluster.setTimeAlerts(removeTimeAlert(cluster, alertId));
        timeAlertRepository.delete(alertId);
        clusterRepository.save(cluster);
    }

    public Set<TimeAlert> removeTimeAlert(Cluster cluster, Long alertId) {
        return cluster.getTimeAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
    }

    public BaseAlert getBaseAlert(Long clusterId, Long alertId) {
        try {
            return findMetricAlertByCluster(clusterId, alertId);
        } catch (RuntimeException ignored) {
            LOGGER.info("Could not found Metric alert with id: '{}', for cluster: '{}'!", alertId, clusterId);
        }
        try {
            return findTimeAlertByCluster(clusterId, alertId);
        } catch (RuntimeException ignored) {
            LOGGER.info("Could not found Time alert with id: '{}', for cluster: '{}'!", alertId, clusterId);
        }
        try {
            return findPrometheusAlertByCluster(clusterId, alertId);
        } catch (RuntimeException ignored) {
            LOGGER.info("Could not found Prometheus alert with id: '{}', for cluster: '{}'!", alertId, clusterId);
        }

        throw new NotFoundException(String.format("Could not found alert with id: '%s', for cluster: '%s'!", alertId, clusterId));
    }

    public BaseAlert save(BaseAlert alert) {
        BaseAlert res = alert;

        if (alert instanceof MetricAlert) {
            res = metricAlertRepository.save((MetricAlert) alert);
        } else if (alert instanceof TimeAlert) {
            res = timeAlertRepository.save((TimeAlert) alert);
        } else if (alert instanceof PrometheusAlert) {
            res = prometheusAlertRepository.save((PrometheusAlert) alert);
        }
        return res;
    }

    public List<Map<String, Object>> getAlertDefinitions(Long clusterId) {
        Cluster cluster = clusterService.findOneById(clusterId);
        List<Map<String, Object>> ret = new ArrayList<>();
        List<Map<String, String>> alertDefinitions = ambariClientProvider.createAmbariClient(cluster).getAlertDefinitions();
        for (Map<String, String> alertDefinition : alertDefinitions) {
            Map<String, Object> tmp = new HashMap<>();
            for (Entry<String, String> stringStringEntry : alertDefinition.entrySet()) {
                tmp.put(stringStringEntry.getKey(), stringStringEntry.getValue());
            }
            ret.add(tmp);
        }
        return ret;
    }

    public void addPeriscopeAlerts(Cluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        if (cluster.getSecurityConfig() != null) {
            try {
                AmbariClient client = ambariClientProvider.createAmbariClient(cluster);
                createAlert(client, getAlertDefinition(client, CONTAINER_ALERT), CONTAINER_ALERT);
                createAlert(client, getAlertDefinition(client, APP_ALERT), APP_ALERT);
            } catch (Exception e) {
                LOGGER.error("Cannot create alert definitions", e);
            }
        }
    }

    public PrometheusAlert createPrometheusAlert(Long clusterId, PrometheusAlert alert) {
        Cluster cluster = clusterService.findOneById(clusterId);
        alert.setCluster(cluster);
        PrometheusAlert savedAlert = (PrometheusAlert) save(alert);
        cluster.addPrometheusAlert(savedAlert);
        clusterRepository.save(cluster);
        consulKeyValueService.addAlert(cluster, savedAlert);
        LOGGER.info("Prometheus alert '{}' has been created for cluster 'ID:{}'", alert.getName(), cluster.getId());
        return savedAlert;
    }

    public PrometheusAlert updatePrometheusAlert(Long clusterId, Long alertId, PrometheusAlert prometheusAlert) {
        PrometheusAlert alert = findPrometheusAlertByCluster(clusterId, alertId);
        alert.setName(prometheusAlert.getName());
        alert.setAlertRule(prometheusAlert.getAlertRule());
        alert.setPeriod(prometheusAlert.getPeriod());
        alert.setDescription(prometheusAlert.getDescription());
        alert.setAlertState(prometheusAlert.getAlertState());
        PrometheusAlert savedAlert = prometheusAlertRepository.save(alert);
        Cluster cluster = clusterService.find(clusterId);
        consulKeyValueService.addAlert(cluster, savedAlert);
        LOGGER.info("Prometheus alert '{}' has been updated for cluster 'ID:{}'", alert.getName(), cluster.getId());
        return savedAlert;
    }

    public PrometheusAlert findPrometheusAlertByCluster(Long clusterId, Long alertId) {
        return prometheusAlertRepository.findByCluster(alertId, clusterId);
    }

    public void deletePrometheusAlert(Long clusterId, Long alertId) {
        PrometheusAlert alert = prometheusAlertRepository.findByCluster(alertId, clusterId);
        Cluster cluster = clusterRepository.findById(clusterId);
        consulKeyValueService.deleteAlert(cluster, alert);
        Set<PrometheusAlert> alerts = cluster.getPrometheusAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
        cluster.setPrometheusAlerts(alerts);
        prometheusAlertRepository.delete(alertId);
        clusterRepository.save(cluster);
        LOGGER.info("Prometheus alert '{}' has been deleted for cluster 'ID:{}'", alert.getName(), cluster.getId());
    }

    public void addPrometheusAlertsToConsul(Cluster cluster) {
        Set<PrometheusAlert> alerts = prometheusAlertRepository.findAllByCluster(cluster.getId());
        alerts.forEach(alert -> consulKeyValueService.addAlert(cluster, alert));
    }

    public List<AlertRuleDefinitionEntry> getPrometheusAlertDefinitions() {
        return prometheusAlertService.getAlertDefinitions();
    }

    public Set<PrometheusAlert> getPrometheusAlerts(Long clusterId) {
        return prometheusAlertRepository.findAllByCluster(clusterId);
    }

    private String getAlertDefinition(CommonService client, String name) throws Exception {
        Map<String, String> model = Collections.singletonMap("clusterName", client.getClusterName());
        return processTemplateIntoString(freemarkerConfiguration.getTemplate(ALERT_PATH + name, "UTF-8"), model);
    }

    private void createAlert(com.sequenceiq.ambari.client.services.AlertService client, String json, String alertName) {
        try {
            client.createAlert(json);
            LOGGER.info("Alert: {} added to the cluster", alertName);
        } catch (Exception ignored) {
            LOGGER.info("Cannot add '{}' to the cluster", alertName);
        }
    }
}