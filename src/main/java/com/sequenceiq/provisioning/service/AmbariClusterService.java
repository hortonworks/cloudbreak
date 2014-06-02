package com.sequenceiq.provisioning.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.InternalServerException;
import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.controller.json.ClusterRequest;
import com.sequenceiq.provisioning.controller.json.ClusterResponse;
import com.sequenceiq.provisioning.controller.json.JsonHelper;
import com.sequenceiq.provisioning.domain.Blueprint;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.BlueprintRepository;
import com.sequenceiq.provisioning.repository.StackRepository;

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

    public void createCluster(User user, Long cloudId, ClusterRequest clusterRequest) {
        try {
            Blueprint blueprint = blueprintRepository.findOne(clusterRequest.getBlueprintId());
            addBlueprint(user, cloudId, blueprint, clusterRequest);
            // TODO: get server and port from cloudService
            AmbariClient ambariClient = new AmbariClient(clusterRequest.getAmbariIp(), PORT);
            // TODO: get hostnames in from cloudService
            //Map<String, String> hosts = ambariClient.getHostNames();

            // ambariClient.getClustersAsJson();
            // TODO: validate that sum cardinality is the same as host.size
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
        // TODO: get server and port from cloudService
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
        try {
            return createClusterJsonFromString(ambariClient.getClusterAsJson());
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

    public void addBlueprint(User user, Long cloudId, Blueprint blueprint, ClusterRequest clusterRequest) {
        // TODO get ambari client host and port from cloud service
        AmbariClient ambariClient = new AmbariClient(clusterRequest.getAmbariIp(), PORT);
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

    public String startAllService(User user, Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
        return "";
    }

    public String stopAllService(User user, Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
        return "";
    }

}
