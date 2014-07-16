package com.sequenceiq.periscope.monitor.event.handler;

import java.util.Date;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfo;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.CapacitySchedulerQueueInfoList;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.ApplicationUpdateEvent;

@Component
public class ApplicationUpdateEventHandler implements ApplicationListener<ApplicationUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationUpdateEventHandler.class);

    @Override
    public void onApplicationEvent(ApplicationUpdateEvent event) {
        for (ApplicationReport report : event.getReports()) {
            printApplicationReport(report);
        }
        SchedulerInfo schedulerInfo = event.getSchedulerInfo();
        if (schedulerInfo instanceof CapacitySchedulerInfo) {
            printCSSchedulerMetrics((CapacitySchedulerInfo) schedulerInfo);
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
