package com.sequenceiq.periscope.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;

@Service
public class AlertService {

    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private MetricAlertRepository metricAlertRepository;
    @Autowired
    private TimeAlertRepository timeAlertRepository;
    @Autowired
    private ClusterService clusterService;

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
        return cluster.newAmbariClient().getAlertDefinitions();
    }
}
