package com.sequenceiq.periscope.service;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.repository.LoadAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;

@Service
public class AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertService.class);

    @Inject
    private TimeAlertRepository timeAlertRepository;

    @Inject
    private LoadAlertRepository loadAlertRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public TimeAlert createTimeAlert(Long clusterId, TimeAlert alert) {
        Cluster cluster = clusterService.findById(clusterId);
        alert.setCluster(cluster);
        alert.setAlertCrn(createAlertCrn(ThreadBasedUserCrnProvider.getAccountId()));
        alert = (TimeAlert) save(alert);
        cluster.addTimeAlert(alert);
        clusterService.save(cluster);
        return alert;
    }

    public TimeAlert findTimeAlertByCluster(Long clusterId, Long alertId) {
        return timeAlertRepository.findByCluster(alertId, clusterId);
    }

    public TimeAlert updateTimeAlert(Long clusterId, Long alertId, TimeAlert timeAlertForUpdate) {
        TimeAlert alert = timeAlertRepository.findByCluster(alertId, clusterId);
        alert.setDescription(timeAlertForUpdate.getDescription());
        alert.setCron(timeAlertForUpdate.getCron());
        alert.setTimeZone(timeAlertForUpdate.getTimeZone());
        alert.setName(timeAlertForUpdate.getName());

        if (StringUtils.isEmpty(alert.getAlertCrn())) {
            alert.setAlertCrn(createAlertCrn(ThreadBasedUserCrnProvider.getAccountId()));
        }

        if (timeAlertForUpdate.getScalingPolicy() != null) {
            alert.getScalingPolicy().setName(timeAlertForUpdate.getScalingPolicy().getName());
            alert.getScalingPolicy().setAdjustmentType(timeAlertForUpdate.getScalingPolicy().getAdjustmentType());
            alert.getScalingPolicy().setScalingAdjustment(timeAlertForUpdate.getScalingPolicy().getScalingAdjustment());
            alert.getScalingPolicy().setHostGroup(timeAlertForUpdate.getScalingPolicy().getHostGroup());
        }
        return timeAlertRepository.save(alert);
    }

    public Set<TimeAlert> getTimeAlerts(Long clusterId) {
        Cluster cluster = clusterService.findById(clusterId);
        return cluster.getTimeAlerts();
    }

    public void deleteTimeAlert(Long clusterId, Long alertId) {
        Cluster cluster = clusterService.findById(clusterId);
        TimeAlert timeAlert = timeAlertRepository.findByCluster(alertId, clusterId);
        cluster.setTimeAlerts(removeTimeAlert(cluster, alertId));
        timeAlertRepository.delete(timeAlert);
        clusterService.save(cluster);
    }

    public Set<TimeAlert> removeTimeAlert(Cluster cluster, Long alertId) {
        return cluster.getTimeAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
    }

    public BaseAlert getBaseAlert(Long clusterId, Long alertId) {
        try {
            return findTimeAlertByCluster(clusterId, alertId);
        } catch (RuntimeException ignored) {
            LOGGER.info("Could not found Time alert with id: '{}', for cluster: '{}'!", alertId, clusterId);
        }
        try {
            return findLoadAlertByCluster(clusterId, alertId);
        } catch (RuntimeException ignored) {
            LOGGER.info("Could not found Load alert with id: '{}', for cluster: '{}'!", alertId, clusterId);
        }

        throw new NotFoundException(String.format("Could not found alert with id: '%s', for cluster: '%s'!", alertId, clusterId));
    }

    public BaseAlert save(BaseAlert alert) {
        BaseAlert res = alert;
        if (alert instanceof TimeAlert) {
            res = timeAlertRepository.save((TimeAlert) alert);
        } else if (alert instanceof LoadAlert) {
            res = loadAlertRepository.save((LoadAlert) alert);
        }
        return res;
    }

    public LoadAlert createLoadAlert(Long clusterId, LoadAlert loadAlert) {
        Cluster cluster = clusterService.findById(clusterId);
        loadAlert.setCluster(cluster);
        loadAlert.setAlertCrn(createAlertCrn(ThreadBasedUserCrnProvider.getAccountId()));
        loadAlert = (LoadAlert) save(loadAlert);
        cluster.addLoadAlert(loadAlert);
        clusterService.save(cluster);
        return loadAlert;
    }

    public LoadAlert updateLoadAlert(Long clusterId, Long alertId, LoadAlert loadAlertForUpdate) {
        LoadAlert alert = loadAlertRepository.findByCluster(alertId, clusterId);
        alert.setName(loadAlertForUpdate.getName());
        alert.setDescription(loadAlertForUpdate.getDescription());
        if (StringUtils.isEmpty(alert.getAlertCrn())) {
            alert.setAlertCrn(createAlertCrn(ThreadBasedUserCrnProvider.getAccountId()));
        }
        if (loadAlertForUpdate.getLoadAlertConfiguration() != null) {
            alert.setLoadAlertConfiguration(loadAlertForUpdate.getLoadAlertConfiguration());
        }
        if (loadAlertForUpdate.getScalingPolicy() != null) {
            alert.getScalingPolicy().setName(loadAlertForUpdate.getScalingPolicy().getName());
            alert.getScalingPolicy().setAdjustmentType(loadAlertForUpdate.getScalingPolicy().getAdjustmentType());
            alert.getScalingPolicy().setScalingAdjustment(loadAlertForUpdate.getScalingPolicy().getScalingAdjustment());
            alert.getScalingPolicy().setHostGroup(loadAlertForUpdate.getScalingPolicy().getHostGroup());
        }
        return loadAlertRepository.save(alert);
    }

    public LoadAlert findLoadAlertByCluster(Long clusterId, Long alertId) {
        return loadAlertRepository.findByCluster(alertId, clusterId);
    }

    public void deleteLoadAlert(Long clusterId, Long alertId) {
        LoadAlert loadAlert = loadAlertRepository.findByCluster(alertId, clusterId);
        Cluster cluster = clusterService.findById(clusterId);
        cluster.setLoadAlerts(removeLoadAlert(cluster, alertId));
        loadAlertRepository.delete(loadAlert);
        clusterService.save(cluster);
    }

    public Set<LoadAlert> removeLoadAlert(Cluster cluster, Long alertId) {
        return cluster.getLoadAlerts().stream().filter(a -> !a.getId().equals(alertId)).collect(Collectors.toSet());
    }

    public Set<LoadAlert> getLoadAlertsForClusterHostGroup(Long clusterId, String hostGroup) {
        Cluster cluster = clusterService.findById(clusterId);
        return cluster.getLoadAlerts().stream()
                .filter(a -> a.getScalingPolicy().getHostGroup().equals(hostGroup)).collect(Collectors.toSet());
    }

    public Set<LoadAlert> getLoadAlerts(Long clusterId) {
        Cluster cluster = clusterService.findById(clusterId);
        return cluster.getLoadAlerts();
    }

    private String createAlertCrn(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.ALERT, accountId);
    }
}
