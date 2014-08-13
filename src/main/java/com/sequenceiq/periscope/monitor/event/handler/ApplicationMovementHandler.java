package com.sequenceiq.periscope.monitor.event.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.apache.hadoop.yarn.exceptions.YarnException;
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
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class ApplicationMovementHandler implements ApplicationListener<ApplicationUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMovementHandler.class);
    private static final int MAX_CAPACITY = 100;

    @Autowired
    private ClusterService clusterService;

    @Override
    public void onApplicationEvent(ApplicationUpdateEvent event) {
        List<ApplicationReport> appReports = event.getReports();
        SchedulerInfo schedulerInfo = event.getSchedulerInfo();

        List<CapacitySchedulerQueueInfo> allQueueInfo = getAllQueueInfo(schedulerInfo);
        Cluster cluster = clusterService.get(event.getClusterId());
        if (cluster == null) {
            return;
        }

        Map<Priority, Map<ApplicationId, SchedulerApplication>> apps = cluster.getApplicationsPriorityOrder();
        Set<ApplicationId> activeApps = new HashSet<>();
        for (ApplicationReport report : appReports) {
            ApplicationId id = report.getApplicationId();
            activeApps.add(id);
            SchedulerApplication application = addApplicationIfAbsent(cluster, report);
            application.update(report);
            if (cluster.isAppMovementAllowed()) {
                apps = cluster.getApplicationsPriorityOrder();
                if (isApplicationHighPriority(apps, id)) {
                    LOGGER.info("Try to move high priority app {}", id);
                    CapacitySchedulerQueueInfo queue = getQueueWithMostAvailableCapacity(cluster, allQueueInfo);
                    if (!application.isMoved()) {
                        float availableMemory = getAvailableResourceCapacity(cluster, queue);
                        int usedMemory = report.getApplicationResourceUsageReport().getUsedResources().getMemory();
                        if (availableMemory - usedMemory > 0) {
                            try {
                                cluster.getYarnClient().moveApplicationAcrossQueues(id, queue.getQueueName());
                                application.setMoved(true);
                                LOGGER.info("Application {} moved to queue {}", id.toString(), queue.getQueueName());
                                break;
                            } catch (IOException | YarnException e) {
                                LOGGER.error("Error moving {} to {}", id.toString(), queue.getQueueName(), e);
                            }
                        }
                    }
                    LOGGER.info("Queue with most available resource : {}", queue.getQueueName());
                }
            }
        }
        removeApplicationIfFinished(cluster, activeApps, apps);

        printQueueReport(allQueueInfo, cluster);
        printApplicationReport(appReports);
    }

    private CapacitySchedulerQueueInfo getQueueWithMostAvailableCapacity(Cluster cluster, List<CapacitySchedulerQueueInfo> allQueueInfo) {
        CapacitySchedulerQueueInfo result = allQueueInfo.get(0);
        int numQueues = allQueueInfo.size();
        if (numQueues > 1) {
            for (int i = 1; i < numQueues; i++) {
                CapacitySchedulerQueueInfo queue = allQueueInfo.get(i);
                if (getAvailableResourceCapacity(cluster, queue) > getAvailableResourceCapacity(cluster, result)) {
                    result = queue;
                }
            }
        }
        return result;
    }

    private List<CapacitySchedulerQueueInfo> getAllQueueInfo(SchedulerInfo schedulerInfo) {
        List<CapacitySchedulerQueueInfo> queueInfoList = new ArrayList<>();
        if (schedulerInfo instanceof CapacitySchedulerInfo) {
            addQueueInfo(queueInfoList, ((CapacitySchedulerInfo) schedulerInfo).getQueues());
        }
        return queueInfoList;
    }

    private void addQueueInfo(List<CapacitySchedulerQueueInfo> queueInfoList, CapacitySchedulerQueueInfoList queues) {
        if (queues != null && queues.getQueueInfoList() != null) {
            for (CapacitySchedulerQueueInfo info : queues.getQueueInfoList()) {
                queueInfoList.add(info);
                addQueueInfo(queueInfoList, info.getQueues());
            }
        }
    }

    private boolean isApplicationHighPriority(Map<Priority, Map<ApplicationId, SchedulerApplication>> apps, ApplicationId id) {
        Map<ApplicationId, SchedulerApplication> high = apps.get(Priority.HIGH);
        return high != null && high.containsKey(id);
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

    private SchedulerApplication addApplicationIfAbsent(Cluster cluster, ApplicationReport appReport) {
        ApplicationId appId = appReport.getApplicationId();
        SchedulerApplication application = cluster.getApplication(appId);
        if (application == null) {
            application = cluster.addApplication(appReport);
        }
        return application;
    }

    private void printQueueReport(List<CapacitySchedulerQueueInfo> infoList, Cluster cluster) {
        for (CapacitySchedulerQueueInfo info : infoList) {
            StringBuilder sb = new StringBuilder();

            sb.append("\nQueue name: ").append(info.getQueueName());
            sb.append("\ncapacity: ").append(info.getCapacity());
            sb.append("\nmax capacity: ").append(info.getMaxCapacity());
            sb.append("\nabsolute capacity: ").append(info.getAbsoluteCapacity());
            sb.append("\nabsolute max capacity: ").append(info.getAbsoluteMaxCapacity());
            sb.append("\nabsolute max resource capacity (MB): ").append(getMaxResourceCapacity(cluster, info));
            sb.append("\nused capacity: ").append(info.getUsedCapacity());
            sb.append("\nabsolute used capacity: ").append(info.getAbsoluteUsedCapacity());
            sb.append("\nnumber of apps: ").append(info.getNumApplications());
            sb.append("\nused resources: ").append(info.getResourcesUsed());

            LOGGER.info(sb.toString());
        }
    }

    private float getMaxResourceCapacity(Cluster cluster, CapacitySchedulerQueueInfo info) {
        return cluster.getTotalMB() * (info.getAbsoluteMaxCapacity() / MAX_CAPACITY);
    }

    private float getAvailableResourceCapacity(Cluster cluster, CapacitySchedulerQueueInfo info) {
        if (info.getQueues() == null) {
            return getMaxResourceCapacity(cluster, info) - info.getResourcesUsed().getMemory();
        }
        return 0;
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
        sb.append("\nstate: ").append(report.getYarnApplicationState());
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

}
