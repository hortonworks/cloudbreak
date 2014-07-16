package com.sequenceiq.periscope.monitor.event.handler;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfoList;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Priority;
import com.sequenceiq.periscope.model.SchedulerApplication;
import com.sequenceiq.periscope.monitor.event.ApplicationUpdateEvent;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Component
public class ApplicationUpdateEventHandler implements ApplicationListener<ApplicationUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationUpdateEventHandler.class);

    @Autowired
    private ClusterRegistry clusterRegistry;

    @Override
    public void onApplicationEvent(ApplicationUpdateEvent event) {
        List<ApplicationReport> appReports = event.getReports();
        SchedulerInfo schedulerInfo = event.getSchedulerInfo();

        Cluster cluster = clusterRegistry.get(event.getClusterId());
        Map<Priority, Map<ApplicationId, SchedulerApplication>> apps = cluster.getApplicationsPriorityOrder();

        Set<ApplicationId> activeApps = new HashSet<>();
        for (ApplicationReport report : appReports) {
            ApplicationId id = report.getApplicationId();
            activeApps.add(id);
            addApplicationIfAbsent(cluster, id);
            if (isApplicationHighPriority(apps, id)) {
                LOGGER.info("Move high priority app {} {}", id, report.getName());
            }
        }
        removeApplicationIfFinished(cluster, activeApps, apps);

        printQueueReport(schedulerInfo);
        printApplicationReport(appReports);
    }

    private boolean isApplicationHighPriority(Map<Priority, Map<ApplicationId, SchedulerApplication>> apps, ApplicationId id) {
        Map<ApplicationId, SchedulerApplication> high = apps.get(Priority.HIGHEST);
        return high == null ? false : high.containsKey(id);
    }

    private void removeApplicationIfFinished(Cluster cluster, Set<ApplicationId> activeApps,
            Map<Priority, Map<ApplicationId, SchedulerApplication>> apps) {
        for (Priority priority : apps.keySet()) {
            for (ApplicationId id : apps.get(priority).keySet()) {
                if (!activeApps.contains(id)) {
                    cluster.removeApplication(id);
                }
            }
        }
    }

    private void addApplicationIfAbsent(Cluster cluster, ApplicationId appId) {
        if (cluster.getApplication(appId) == null) {
            cluster.addApplication(appId, Priority.NORMAL);
        }
    }

    private void printQueueReport(SchedulerInfo schedulerInfo) {
        if (schedulerInfo instanceof CapacitySchedulerInfo) {
            printCSSchedulerMetrics((CapacitySchedulerInfo) schedulerInfo);
        }
    }

    private void printApplicationReport(List<ApplicationReport> reports) {
        for (ApplicationReport report : reports) {
            printApplicationReport(report);
        }
    }

    private void printApplicationReport(ApplicationReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("\nApplication: ").append(report.getApplicationId());
        sb.append("\ntype: ").append(report.getApplicationType());
        sb.append("\nqueue: ").append(report.getQueue());
        sb.append("\nstart time: ").append(new Date(report.getStartTime()));
        sb.append("\nprogress: ").append(report.getProgress());
        sb.append("\nuser: ").append(report.getUser());

        ApplicationResourceUsageReport usage = report.getApplicationResourceUsageReport();
        sb.append("\nreserved containers: ").append(usage.getNumReservedContainers());
        sb.append("\nreserved resources: ").append(usage.getReservedResources());
        sb.append("\nneeded resource: ").append(usage.getNeededResources());
        sb.append("\nused containers: ").append(usage.getNumUsedContainers());
        sb.append("\nused resources").append(usage.getUsedResources());

        LOGGER.info(sb.toString());
    }

    private void printCSSchedulerMetrics(CapacitySchedulerInfo schedulerInfo) {
        printCSSchedulerMetrics(schedulerInfo.getQueues());
    }

    private void printCSSchedulerMetrics(CapacitySchedulerQueueInfoList infoList) {
        if (infoList != null && infoList.getQueueInfoList() != null) {
            for (CapacitySchedulerQueueInfo info : infoList.getQueueInfoList()) {
                StringBuilder sb = new StringBuilder();

                sb.append("\nQueue name: ").append(info.getQueueName());
                sb.append("\ncapacity: ").append(info.getCapacity());
                sb.append("\nmax capacity: ").append(info.getMaxCapacity());
                sb.append("\nabsolute capacity: ").append(info.getAbsoluteCapacity());
                sb.append("\nabsolute max capacity: ").append(info.getAbsoluteMaxCapacity());
                sb.append("\nused capacity: ").append(info.getUsedCapacity());
                sb.append("\nabsolute used capacity: ").append(info.getAbsoluteUsedCapacity());
                sb.append("\nnumber of apps: ").append(info.getNumApplications());
                sb.append("\nused resources: ").append(info.getResourcesUsed());
                sb.append("\nused capacity: ").append(info.getUsedCapacity());

                LOGGER.info(sb.toString());
                printCSSchedulerMetrics(info.getQueues());
            }
        }
    }

}
