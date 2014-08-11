package com.sequenceiq.periscope.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Priority;
import com.sequenceiq.periscope.model.SchedulerApplication;
import com.sequenceiq.periscope.registry.Cluster;

@Service
public class AppService {

    @Autowired
    private ClusterService clusterService;

    public List<ApplicationReport> getApplicationReports(String clusterId) throws IOException, YarnException {
        List<ApplicationReport> result = Collections.emptyList();
        Cluster cluster = clusterService.get(clusterId);
        if (cluster != null) {
            YarnClient yarnClient = cluster.getYarnClient();
            result = yarnClient.getApplications();
        }
        return result;
    }

    public void setPriorityToHighRandomly(String clusterId) {
        Cluster cluster = clusterService.get(clusterId);
        if (cluster != null) {
            Map<ApplicationId, SchedulerApplication> applications = cluster.getApplications(Priority.NORMAL);
            int i = 0;
            for (ApplicationId applicationId : applications.keySet()) {
                if (i++ % 2 == 0) {
                    cluster.setApplicationPriority(applicationId, Priority.HIGH);
                }
            }
        }
    }

}
