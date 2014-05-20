package com.sequenceiq.provisioning.service;

import groovyx.net.http.HttpResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.provisioning.controller.InternalServerException;
import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.ClusterRequest;
import com.sequenceiq.provisioning.controller.json.ClusterResponse;
import com.sequenceiq.provisioning.controller.json.HostGroupMappingJson;
import com.sequenceiq.provisioning.domain.User;

@Service
public class AmbariClusterService {

    public void createCluster(User user, Long cloudId, ClusterRequest clusterRequest) {
        try {
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
            ambariClient.createCluster(clusterRequest.getClusterName(), clusterRequest.getBlueprintName(), hostGroupMappings);
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
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser jp = factory.createParser(cluster);
        JsonNode actualObj = mapper.readTree(jp);
        clusterResponse.setCluster(actualObj);
        return clusterResponse;
    }
}
