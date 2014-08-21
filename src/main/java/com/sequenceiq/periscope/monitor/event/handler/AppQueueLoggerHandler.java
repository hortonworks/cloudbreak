package com.sequenceiq.periscope.monitor.event.handler;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeMaxQueueResourceCapacity;
import static com.sequenceiq.periscope.utils.ClusterUtils.getAllQueueInfo;

import java.util.Date;
import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.ApplicationUpdateEvent;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class AppQueueLoggerHandler implements ApplicationListener<ApplicationUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppQueueLoggerHandler.class);

    @Autowired
    private ClusterService clusterService;

    @Override
    public void onApplicationEvent(ApplicationUpdateEvent event) {
        try {
            Cluster cluster = clusterService.get(event.getClusterId());
            printQueueReport(getAllQueueInfo(event.getSchedulerInfo()), cluster);
            printApplicationReport(event.getReports(), cluster);
        } catch (ClusterNotFoundException e) {
            LOGGER.error("Cluster not found and cannot be logged, id: " + event.getClusterId(), e);
        }
    }

    private void printQueueReport(List<CapacitySchedulerQueueInfo> infoList, Cluster cluster) {
        StringBuilder sb = new StringBuilder();
        for (CapacitySchedulerQueueInfo queue : infoList) {
            sb.append("\nQueue name: ").append(queue.getQueueName());
            sb.append("\ncapacity: ").append(queue.getCapacity());
            sb.append("\nmax capacity: ").append(queue.getMaxCapacity());
            sb.append("\nabsolute capacity: ").append(queue.getAbsoluteCapacity());
            sb.append("\nabsolute max capacity: ").append(queue.getAbsoluteMaxCapacity());
            sb.append("\nabsolute max resource capacity (MB): ").append(computeMaxQueueResourceCapacity(cluster, queue));
            sb.append("\nused capacity: ").append(queue.getUsedCapacity());
            sb.append("\nabsolute used capacity: ").append(queue.getAbsoluteUsedCapacity());
            sb.append("\nnumber of apps: ").append(queue.getNumApplications());
            sb.append("\nused resources: ").append(queue.getResourcesUsed());
            sb.append("\n");
        }
        LOGGER.info("Queue update of {} {}", cluster.getId(), sb);
    }

    private void printApplicationReport(List<ApplicationReport> reports, Cluster cluster) {
        StringBuilder sb = new StringBuilder();
        for (ApplicationReport report : reports) {
            printApplicationReport(report, sb);
        }
        LOGGER.info("Application update of {} {}", cluster.getId(), sb);
    }

    private void printApplicationReport(ApplicationReport report, StringBuilder sb) {
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
        sb.append("\n");
    }
}
