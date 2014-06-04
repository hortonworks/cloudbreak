package com.sequenceiq.cloudbreak.service;

import groovyx.net.http.HttpResponseException;

import java.math.BigDecimal;

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
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.ClusterStatusMessage;

@Service
public class AmbariClusterInstaller {

    @Autowired
    private WebsocketService websocketService;

    @Async
    public void installAmbariCluster(Stack stack) {
        try {
            Cluster cluster = stack.getCluster();
            if (stack.getCluster() != null && stack.getCluster().getStatus().equals(Status.REQUESTED)) {
                // Blueprint blueprint =
                // blueprintRepository.findOne(clusterRequest.getBlueprintId());
                Blueprint blueprint = cluster.getBlueprint();
                addBlueprint(stack.getAmbariIp(), blueprint);
                AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), AmbariClusterService.PORT);
                ambariClient.createCluster(
                        cluster.getName(),
                        blueprint.getName(),
                        ambariClient.recommendAssignments(blueprint.getName())
                        );

                BigDecimal installProgress = new BigDecimal(0);
                while (installProgress.doubleValue() != 100.0) {
                    installProgress = ambariClient.getInstallProgress();
                    // TODO: timeout
                }
                websocketService.send("/topic/cluster", new ClusterStatusMessage(cluster.getName(), Status.CREATE_COMPLETED.name()));
            }

        } catch (HttpResponseException e) {
            throw new InternalServerException("Failed to create cluster", e);
        }
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
