package com.sequenceiq.provisioning.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.provisioning.controller.json.HostGroupMappingJson;
import com.sequenceiq.provisioning.controller.json.JsonHelper;
import com.sequenceiq.provisioning.domain.Blueprint;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.BlueprintRepository;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterService {

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private BlueprintRepository blueprintRepository;

    public void createCluster(User user, Long cloudId, ClusterRequest clusterRequest) {
        try {
            addBlueprint(user, cloudId, blueprintRepository.findOne(clusterRequest.getBlueprintId()));
            // TODO: get server and port from cloudService
            AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
            // TODO: get hostnames in from cloudService
            List<String> hosts = Arrays.asList("node1.mykorp.kom", "node2.mykorp.kom", "node3.mykorp.kom", "node4.mykorp.kom");

            // TODO: validate that sum cardinality is the same as host.size
            Map<String, List<String>> hostGroupMappings = new HashMap<>();
            int hostIndex = 0;
            for (HostGroupMappingJson hostGroupMappingJson : clusterRequest.getHostGroups()) {
                List<String> hostsInGroup = new ArrayList<>();
                for (int i = 0; i < hostGroupMappingJson.getCardinality(); i++) {
                    hostsInGroup.add(hosts.get(hostIndex++));
                }
                hostGroupMappings.put(hostGroupMappingJson.getName(), hostsInGroup);
            }
            ambariClient.createCluster(
                    clusterRequest.getClusterName(),
                    blueprintRepository.findOne(clusterRequest.getBlueprintId()).getName(),
                    hostGroupMappings
            );

        } catch (HttpResponseException e) {
            throw new InternalServerException("Failed to create cluster", e);
        }
    }

    public ClusterResponse retrieveCluster(User user, Long cloudId) {
        // TODO: get server and port from cloudService
        AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
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

    public void addBlueprint(User user, Long cloudId, Blueprint blueprint) {
        // TODO get ambari client host and port from cloud service
        AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
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

    public List<BlueprintJson> retrieveBlueprints(User user, Long cloudId) {
        try {
            List<BlueprintJson> blueprints = new ArrayList<>();
            // TODO get ambari client host and port from cloud service
            AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
            Set<String> blueprintNames = ambariClient.getBlueprintsMap().keySet();
            for (String blueprintName : blueprintNames) {
                blueprints.add(createBlueprintJsonFromString(ambariClient.getBlueprintAsJson(blueprintName)));
            }
            return blueprints;
        } catch (IOException e) {
            throw new InternalServerException("Jackson failed to parse blueprint JSON.", e);
        }
    }

    public BlueprintJson retrieveBlueprint(User user, Long cloudId, String id) {
        // TODO get ambari client host and port from cloud service
        AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
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
        //todo define startall
        AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
        return "";
    }

    public String stopAllService(User user, Long stackId) {
        //todo define stopall
        AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
        return "";
    }

}
