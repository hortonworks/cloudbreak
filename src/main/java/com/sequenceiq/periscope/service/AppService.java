package com.sequenceiq.periscope.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Priority;
import com.sequenceiq.periscope.model.SchedulerApplication;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Service
public class AppService {

    @Autowired
    private ClusterRegistry clusterRegistry;

    public void moveToQueue(String clusterId, String appId, String queue) throws IOException, YarnException {
        Cluster cluster = getCluster(clusterId);
        YarnClient yarnClient = cluster.getYarnClient();
        ApplicationId applicationId = ConverterUtils.toApplicationId(appId);
        yarnClient.moveApplicationAcrossQueues(applicationId, queue);
    }

    public List<ApplicationReport> getApplicationReports(String clusterId) throws IOException, YarnException {
        Cluster cluster = getCluster(clusterId);
        YarnClient yarnClient = cluster.getYarnClient();
        return yarnClient.getApplications();
    }

    public void setPriorityToHighRandomly(String clusterId) {
        Cluster cluster = getCluster(clusterId);
        Map<ApplicationId, SchedulerApplication> applications = cluster.getApplications(Priority.NORMAL);
        int i = 0;
        for (ApplicationId applicationId : applications.keySet()) {
            if (i++ % 2 == 0) {
                cluster.setPriority(applicationId, Priority.HIGHEST);
            }
        }
    }

    public SchedulerApplication addApplication(String clusterId, Priority priority, String appId) {
        Cluster cluster = getCluster(clusterId);
        return cluster.addApplication(appId, priority);
    }

    private Cluster getCluster(String clusterId) {
        Cluster cluster = clusterRegistry.get(clusterId);
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return cluster;
    }

}
