package com.sequenceiq.cloudbreak.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.aws.AwsProvisionService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterInstaller {

    private static final double COMPLETED = 100.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisionService.class);

    private static final long POLLING_INTERVAL = 3000;
    private static final int MILLIS = 1000;

    @Autowired
    private WebsocketService websocketService;

    @Async
    public void installAmbariCluster(Stack stack) {
        try {
            Cluster cluster = stack.getCluster();
            if (stack.getCluster() != null && stack.getCluster().getStatus().equals(Status.REQUESTED)) {
                Blueprint blueprint = cluster.getBlueprint();
                addBlueprint(stack.getAmbariIp(), blueprint);
                AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), AmbariClusterService.PORT);
                ambariClient.createCluster(
                        cluster.getName(),
                        blueprint.getName(),
                        recommend(stack, ambariClient, blueprint.getName())
                );

                BigDecimal installProgress = new BigDecimal(0);
                while (installProgress.doubleValue() != COMPLETED) {
                    try {
                        Thread.sleep(POLLING_INTERVAL);
                    } catch (InterruptedException e) {
                        LOGGER.info("Interrupted exception occured during polling.", e);
                        Thread.currentThread().interrupt();
                    }
                    installProgress = ambariClient.getInstallProgress();
                    LOGGER.info("Ambari Cluster installing. [Stack: '{}', Cluster: '{}', Progress: {}]", stack.getId(), cluster.getName(), installProgress);
                    // TODO: timeout
                }
                websocketService.send("/topic/cluster", new StatusMessage(cluster.getId(), cluster.getName(), Status.CREATE_COMPLETED.name()));
            } else {
                LOGGER.info("There were no cluster request to this stack, won't install cluster now. [stack: {}]", stack.getId());
            }

        } catch (HttpResponseException e) {
            throw new InternalServerException("Failed to create cluster", e);
        }
    }

    private Map<String, List<String>> recommend(Stack stack, AmbariClient ambariClient, String blueprintName) {
        Map<String, List<String>> stringListMap = ambariClient.recommendAssignments(blueprintName);
        int nodeCount = 0;
        while (nodeCount != stack.getNodeCount()) {
            nodeCount = 0;
            stringListMap = ambariClient.recommendAssignments(blueprintName);
            try {
                Thread.sleep(MILLIS);
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted exception in recommendation. stackId: {} blueprintName: {} exception: {}", stack.getId(), blueprintName, e);
            }
            for (Map.Entry<String, List<String>> s : stringListMap.entrySet()) {
                nodeCount += s.getValue().size();
            }
        }
        return stringListMap;
    }

    public void addBlueprint(String ambariIp, Blueprint blueprint) {
        AmbariClient ambariClient = new AmbariClient(ambariIp, AmbariClusterService.PORT);
        try {
            ambariClient.addBlueprint(blueprint.getBlueprintText());
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Ambari blueprint already exists.", e);
            } else if ("Bad Request".equals(e.getMessage())) {
                throw new BadRequestException("Failed to validate Ambari blueprint.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

}
