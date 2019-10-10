package com.sequenceiq.periscope.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.api.model.AlertRuleDefinitionEntry;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.PrometheusAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
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

    @Inject
    private AmbariRequestLogging ambariRequestLogging;

    public MetricAlert createMetricAlert(Long clusterId, MetricAlert alert) {
        Cluster cluster = clusterService.findById(clusterId);
        validateNewAlert(cluster, alert);
        alert.setCluster(cluster);
        MetricAlert metricAlert = (MetricAlert) save(alert);
        cluster.addMetricAlert(metricAlert);
        clusterService.save(cluster);
        return metricAlert;
    }

    public MetricAlert updateMetricAlert(Long clusterId, Long alertId, MetricAlert metricAlert) {
        Cluster cluster = clusterService.findById(clusterId);
        Optional<MetricAlert> existingAlert = findAlert(cluster.getMetricAlerts(), alertId);
        if (existingAlert.isPresent()) {
            MetricAlert alert = existingAlert.get();
            validateExistingAlert(cluster, metricAlert, alert);
            alert.setName(metricAlert.getName());
            alert.setDefinitionName(metricAlert.getDefinitionName());
            alert.setPeriod(metricAlert.getPeriod());
            alert.setDescription(metricAlert.getDescription());
            alert.setAlertState(metricAlert.getAlertState());
            return metricAlertRepository.save(alert);
        } else {
            throw new BadRequestException(String.format("The metric alert with id %s does not exist", alertId));
        }
    }

    public void deleteMetricAlert(Long clusterId, Long alertId) {
        Cluster cluster = clusterService.findById(clusterId);
        cluster.setMetricAlerts(removeMetricAlert(cluster, alertId));
        clusterService.save(cluster);
    }

    public Set<MetricAlert> getMetricAlerts(Long clusterId) {
        return new HashSet<>(metricAlertRepository.findAllByCluster(clusterId));
    }

    public TimeAlert createTimeAlert(Long clusterId, TimeAlert alert) {
        Cluster cluster = clusterService.findById(clusterId);
        validateNewAlert(cluster, alert);
        alert.setCluster(cluster);
        alert = (TimeAlert) save(alert);
        cluster.addTimeAlert(alert);
        clusterService.save(cluster);
        return alert;
    }

    public TimeAlert updateTimeAlert(Long clusterId, Long alertId, TimeAlert timeAlert) {
        Cluster cluster = clusterService.findById(clusterId);
        Optional<TimeAlert> existingAlert = findAlert(cluster.getTimeAlerts(), alertId);
        if (existingAlert.isPresent()) {
            TimeAlert alert = existingAlert.get();
            validateExistingAlert(cluster, timeAlert, alert);
            alert.setDescription(timeAlert.getDescription());
            alert.setCron(timeAlert.getCron());
            alert.setTimeZone(timeAlert.getTimeZone());
            alert.setName(timeAlert.getName());
            return timeAlertRepository.save(alert);
        } else {
            throw new BadRequestException(String.format("The time alert with id %s does not exist", alertId));
        }
    }

    public Set<TimeAlert> getTimeAlerts(Long clusterId) {
        return new HashSet<>(timeAlertRepository.findAllByCluster(clusterId));
    }

    public void deleteTimeAlert(Long clusterId, Long alertId) {
        Cluster cluster = clusterService.findById(clusterId);
        cluster.setTimeAlerts(removeTimeAlert(cluster, alertId));
        clusterService.save(cluster);
    }

    public PrometheusAlert createPrometheusAlert(Long clusterId, PrometheusAlert alert) {
        Cluster cluster = clusterService.findById(clusterId);
        validateNewAlert(cluster, alert);
        alert.setCluster(cluster);
        PrometheusAlert savedAlert = (PrometheusAlert) save(alert);
        cluster.addPrometheusAlert(savedAlert);
        clusterService.save(cluster);
        consulKeyValueService.addAlert(cluster, savedAlert);
        LOGGER.info("Prometheus alert '{}' has been created for cluster 'ID:{}'", alert.getName(), cluster.getId());
        return savedAlert;
    }

    public PrometheusAlert updatePrometheusAlert(Long clusterId, Long alertId, PrometheusAlert prometheusAlert) {
        Cluster cluster = clusterService.findById(clusterId);
        Optional<PrometheusAlert> existingAlert = findAlert(cluster.getPrometheusAlerts(), alertId);
        if (existingAlert.isPresent()) {
            PrometheusAlert alert = existingAlert.get();
            validateExistingAlert(cluster, prometheusAlert, alert);
            alert.setName(prometheusAlert.getName());
            alert.setAlertRule(prometheusAlert.getAlertRule());
            alert.setPeriod(prometheusAlert.getPeriod());
            alert.setDescription(prometheusAlert.getDescription());
            alert.setAlertState(prometheusAlert.getAlertState());
            PrometheusAlert savedAlert = prometheusAlertRepository.save(alert);
            consulKeyValueService.addAlert(cluster, savedAlert);
            LOGGER.info("Prometheus alert '{}' has been updated for cluster 'ID:{}'", alert.getName(), cluster.getId());
            return savedAlert;
        } else {
            throw new BadRequestException(String.format("The prometheus alert with id %s does not exist", alertId));
        }
    }

    public void deletePrometheusAlert(Long clusterId, Long alertId) {
        PrometheusAlert alert = prometheusAlertRepository.findByCluster(alertId, clusterId);
        Cluster cluster = clusterService.findById(clusterId);
        consulKeyValueService.deleteAlert(cluster, alert);
        Set<PrometheusAlert> alerts = cluster.getPrometheusAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
        cluster.setPrometheusAlerts(alerts);
        clusterService.save(cluster);
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

    public BaseAlert getBaseAlert(Long clusterId, Long alertId) {
        try {
            return metricAlertRepository.findByCluster(alertId, clusterId);
        } catch (RuntimeException ignored) {
            LOGGER.info("Could not found Metric alert with id: '{}', for cluster: '{}'!", alertId, clusterId);
        }
        try {
            return timeAlertRepository.findByCluster(alertId, clusterId);
        } catch (RuntimeException ignored) {
            LOGGER.info("Could not found Time alert with id: '{}', for cluster: '{}'!", alertId, clusterId);
        }
        try {
            return prometheusAlertRepository.findByCluster(alertId, clusterId);
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
        Cluster cluster = clusterService.findById(clusterId);
        List<Map<String, Object>> ret = new ArrayList<>();
        AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
        List<Map<String, String>> alertDefinitions = ambariRequestLogging.logging(ambariClient::getAlertDefinitions, "alertDefinition");
        for (Map<String, String> alertDefinition : alertDefinitions) {
            Map<String, Object> tmp = new HashMap<>();
            for (Entry<String, String> stringStringEntry : alertDefinition.entrySet()) {
                tmp.put(stringStringEntry.getKey(), stringStringEntry.getValue());
            }
            ret.add(tmp);
        }
        return ret;
    }

    private Set<MetricAlert> removeMetricAlert(Cluster cluster, Long alertId) {
        return cluster.getMetricAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
    }

    private Set<TimeAlert> removeTimeAlert(Cluster cluster, Long alertId) {
        return cluster.getTimeAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
    }

    private <T extends BaseAlert> Optional<T> findAlert(Set<T> alerts, Long alertId) {
        return alerts.stream().filter(alert -> alert.getId().equals(alertId)).findFirst();
    }

    private void validateExistingAlert(Cluster cluster, BaseAlert newAlert, BaseAlert toBeModifiedAlert) {
        Optional<? extends BaseAlert> existingAlert = findAlertWithTheSameName(cluster, newAlert);
        if (existingAlert.isPresent()) {
            BaseAlert alert = existingAlert.get();
            boolean sameType = alert.getClass().equals(toBeModifiedAlert.getClass());
            if (!sameType || !alert.getId().equals(toBeModifiedAlert.getId())) {
                throw new BadRequestException(String.format("The alert is already existing with the %s name", alert.getName()));
            }
        }
    }

    private void validateNewAlert(Cluster cluster, BaseAlert newAlert) {
        if (findAlertWithTheSameName(cluster, newAlert).isPresent()) {
            throw new BadRequestException(String.format("An alert is already existing with the %s name", newAlert.getName()));
        }
    }

    private Optional<? extends BaseAlert> findAlertWithTheSameName(Cluster cluster, BaseAlert newAlert) {
        String name = newAlert.getName();
        Optional<? extends BaseAlert> existingAlert = cluster.getMetricAlerts().stream().filter(ma -> ma.getName().equals(name)).findFirst();
        if (existingAlert.isEmpty()) {
            existingAlert = cluster.getTimeAlerts().stream().filter(ta -> ta.getName().equals(name)).findFirst();
        }
        if (existingAlert.isEmpty()) {
            existingAlert = cluster.getPrometheusAlerts().stream().filter(pa -> pa.getName().equals(name)).findFirst();
        }
        return existingAlert;
    }
}
