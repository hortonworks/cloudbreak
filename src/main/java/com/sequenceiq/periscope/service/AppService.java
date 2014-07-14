package com.sequenceiq.periscope.service;

import java.io.IOException;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Service
public class AppService {

    @Autowired
    private ClusterRegistry clusterRegistry;

    public void moveToQueue(String clusterId, String appId, String queue) throws YarnException {
        ClusterRegistration registration = getClusterRegistration(clusterId);
        YarnClient yarnClient = registration.getYarnClient();
        try {
            ApplicationId applicationId = ConverterUtils.toApplicationId(appId);
            yarnClient.moveApplicationAcrossQueues(applicationId, queue);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private ClusterRegistration getClusterRegistration(String clusterId) {
        ClusterRegistration registration = clusterRegistry.get(clusterId);
        if (registration == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return registration;
    }

}
