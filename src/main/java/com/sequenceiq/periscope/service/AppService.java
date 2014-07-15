package com.sequenceiq.periscope.service;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public List<ApplicationReport> getApps(String clusterId) throws IOException, YarnException {
        Cluster cluster = getCluster(clusterId);
        YarnClient yarnClient = cluster.getYarnClient();
        return yarnClient.getApplications();
    }

    private Cluster getCluster(String clusterId) {
        Cluster cluster = clusterRegistry.get(clusterId);
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return cluster;
    }

}
