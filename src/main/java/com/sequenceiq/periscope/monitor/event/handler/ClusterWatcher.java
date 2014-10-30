package com.sequenceiq.periscope.monitor.event.handler;

import java.text.DecimalFormat;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlarm;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ComparisonOperator;
import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.NotificationService;
import com.sequenceiq.periscope.service.ScalingService;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.DateUtils;

@Component
public class ClusterWatcher implements ApplicationListener<ClusterMetricsUpdateEvent> {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ClusterWatcher.class);
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("##.##");

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScalingService scalingService;
    @Autowired
    private NotificationService notificationService;

    @Override
    public void onApplicationEvent(ClusterMetricsUpdateEvent event) {
        long clusterId = event.getClusterId();
        try {
            Cluster cluster = clusterService.get(clusterId);
            ClusterMetricsInfo metrics = event.getClusterMetricsInfo();
            cluster.updateMetrics(metrics);
            checkTimeAlarms(cluster);
            checkMetricAlarms(cluster, metrics);
        } catch (ClusterNotFoundException e) {
            LOGGER.error(clusterId, "Cluster not found", e);
        }
    }

    private void checkTimeAlarms(Cluster cluster) {
        for (TimeAlarm alarm : cluster.getTimeAlarms()) {
            long clusterId = cluster.getId();
            String alarmName = alarm.getName();
            LOGGER.info(clusterId, "Checking time based alarm: '{}'", alarmName);
            if (isTrigger(alarm, clusterId)) {
                LOGGER.info(clusterId, "Alarm: '{}' triggers", alarmName);
                handleNotifications(cluster, alarm);
                handleScaling(cluster, alarm);
                break;
            }
        }
    }

    private boolean isTrigger(TimeAlarm alarm, long clusterId) {
        return DateUtils.isTrigger(clusterId, alarm.getCron(), alarm.getTimeZone(), MonitorUpdateRate.CLUSTER_UPDATE_RATE);
    }

    private void checkMetricAlarms(Cluster cluster, ClusterMetricsInfo metrics) {
        for (MetricAlarm metricAlarm : cluster.getMetricAlarms()) {
            long clusterId = cluster.getId();
            LOGGER.info(clusterId, "Checking metric based alarm: '{}'", metricAlarm.getName());
            double value = getMetricValue(metrics, metricAlarm);
            if (alarmHit(value, metricAlarm, clusterId)) {
                handleNotifications(cluster, metricAlarm);
                handleScaling(cluster, metricAlarm);
            }
        }
    }

    private double getMetricValue(ClusterMetricsInfo metrics, MetricAlarm metricAlarm) {
        switch (metricAlarm.getMetric()) {
            case PENDING_CONTAINERS:
                return metrics.getPendingContainers();
            case PENDING_APPLICATIONS:
                return metrics.getAppsPending();
            case LOST_NODES:
                return metrics.getLostNodes();
            case UNHEALTHY_NODES:
                return metrics.getUnhealthyNodes();
            case GLOBAL_RESOURCES:
                return ClusterUtils.computeFreeClusterResourceRate(metrics);
            default:
                return 0;
        }
    }

    private boolean alarmHit(double value, MetricAlarm metricAlarm, long clusterId) {
        switch (metricAlarm.getComparisonOperator()) {
            case EQUALS:
                return isEqualsHits(value, metricAlarm, clusterId);
            case GREATER_OR_EQUAL_THAN:
                return isGreaterOrEqualHits(value, metricAlarm, clusterId);
            case GREATER_THAN:
                return isGreaterHits(value, metricAlarm, clusterId);
            case LESS_OR_EQUAL_THAN:
                return isLessOrEqualHits(value, metricAlarm, clusterId);
            case LESS_THAN:
                return isLessHits(value, metricAlarm, clusterId);
            default:
                return false;
        }
    }

    private boolean isEqualsHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value == metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.EQUALS, clusterId);
    }

    private boolean isGreaterOrEqualHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value >= metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.GREATER_OR_EQUAL_THAN, clusterId);
    }

    private boolean isGreaterHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value > metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.GREATER_THAN, clusterId);
    }

    private boolean isLessOrEqualHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value <= metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.LESS_OR_EQUAL_THAN, clusterId);
    }

    private boolean isLessHits(double value, MetricAlarm metricAlarm, long clusterId) {
        return isComparisonHit(value < metricAlarm.getThreshold(), metricAlarm, ComparisonOperator.LESS_THAN, clusterId);
    }

    private boolean isComparisonHit(boolean valueHit, MetricAlarm metricAlarm, ComparisonOperator operator, long clusterId) {
        boolean result = false;
        String alarmName = metricAlarm.getName();
        if (valueHit) {
            LOGGER.info(clusterId, "{} comparison hit for alarm: '{}'", operator, alarmName);
            result = setAndCheckTime(metricAlarm, clusterId);
        } else {
            LOGGER.info(clusterId, "{} comparison failed for alarm: '{}'", operator, alarmName);
            reset(metricAlarm);
        }
        return result;
    }

    private boolean setAndCheckTime(MetricAlarm metricAlarm, long clusterId) {
        boolean result = false;
        String alarmName = metricAlarm.getName();
        long hitsSince = metricAlarm.getAlarmHitsSince();
        if (hitsSince == 0) {
            LOGGER.info(clusterId, "Counter starts until hit for alarm: '{}'", alarmName);
            setCurrentTime(metricAlarm);
        } else {
            long elapsedTime = System.currentTimeMillis() - hitsSince;
            result = elapsedTime > (metricAlarm.getPeriod() * ClusterUtils.MIN_IN_MS);
            LOGGER.info(clusterId, "Alarm: '{}' stands since {} minutes", alarmName,
                    TIME_FORMAT.format((double) elapsedTime / ClusterUtils.MIN_IN_MS));
            if (result) {
                LOGGER.info(clusterId, "Alarm: '{}' HIT", alarmName);
            }
        }
        return result;
    }

    private void reset(MetricAlarm metricAlarm) {
        metricAlarm.reset();
    }

    private void setCurrentTime(MetricAlarm metricAlarm) {
        metricAlarm.setAlarmHitsSince(System.currentTimeMillis());
    }

    private void handleScaling(Cluster cluster, BaseAlarm alarm) {
        ScalingPolicy scalingPolicy = alarm.getScalingPolicy();
        if (scalingPolicy != null) {
            scalingService.scale(cluster, scalingPolicy);
        }
    }

    private void handleNotifications(Cluster cluster, BaseAlarm alarm) {
        if (!alarm.isNotificationSent()) {
            for (Notification notification : alarm.getNotifications()) {
                notificationService.sendNotification(cluster, alarm, notification);
            }
            alarm.setNotificationSent(true);
        }
    }

}