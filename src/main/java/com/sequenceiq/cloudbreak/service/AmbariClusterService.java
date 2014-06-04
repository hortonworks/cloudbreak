package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterService {

    private static final String PORT = "8080";

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private StackRepository stackRepository;

    @Async
    public void createCluster(User user, Long stackId, ClusterRequest clusterRequest) {
        try {
            Stack stack = stackRepository.findOne(stackId);
            Blueprint blueprint = blueprintRepository.findOne(clusterRequest.getBlueprintId());
            addBlueprint(user, stackId, blueprint, clusterRequest);
            AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
            ambariClient.createCluster(
                    clusterRequest.getClusterName(),
                    blueprint.getName(),
                    ambariClient.recommendAssignments(blueprint.getName())
                    );

        } catch (HttpResponseException e) {
            throw new InternalServerException("Failed to create cluster", e);
        }
    }

    public ClusterResponse retrieveCluster(User user, Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
        try {
            String clusterJson = ambariClient.getClusterAsJson();
            if (clusterJson == null) {
                throw new InternalServerException(String.format("Cluster response coming from Ambari server was null. [Stack: '%s', Ambari Server IP: '%s']",
                        stackId, stack.getAmbariIp()));
            }
            return createClusterJsonFromString(clusterJson);
        } catch (HttpResponseException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new NotFoundException("Ambari blueprint not found.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        } catch (IOException e) {
            throw new InternalServerException("Failed to parse cluster json coming from Ambari.", e);
        }
    }

    private ClusterResponse createClusterJsonFromString(String cluster) throws IOException {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setCluster(jsonHelper.createJsonFromString(cluster));
        return clusterResponse;
    }

    public void addBlueprint(User user, Long stackId, Blueprint blueprint, ClusterRequest clusterRequest) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
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

    public List<BlueprintJson> retrieveBlueprints(User user, Long stackId) {
        try {
            List<BlueprintJson> blueprints = new ArrayList<>();
            Stack stack = stackRepository.findOne(stackId);
            AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
            Set<String> blueprintNames = ambariClient.getBlueprintsMap().keySet();
            for (String blueprintName : blueprintNames) {
                blueprints.add(createBlueprintJsonFromString(ambariClient.getBlueprintAsJson(blueprintName)));
            }
            return blueprints;
        } catch (IOException e) {
            throw new InternalServerException("Jackson failed to parse blueprint JSON.", e);
        }
    }

    public BlueprintJson retrieveBlueprint(User user, Long stackId, String id) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
        try {
            return createBlueprintJsonFromString(ambariClient.getBlueprintAsJson(id));
        } catch (HttpResponseException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new NotFoundException("Ambari blueprint not found.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        } catch (IOException e) {
            throw new InternalServerException("Jackson failed to parse blueprint JSON.", e);
        }
    }

    private BlueprintJson createBlueprintJsonFromString(String blueprint) throws IOException {
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(blueprint));
        return blueprintJson;
    }

    @Async
    public void startAllService(User user, Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
        ambariClient.startAllServices();
    }

    @Async
    public void stopAllService(User user, Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
        ambariClient.stopAllServices();
    }

}
