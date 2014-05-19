package com.sequenceiq.provisioning.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.provisioning.controller.json.ClusterJson;
import com.sequenceiq.provisioning.controller.json.HostGroupMappingJson;
import com.sequenceiq.provisioning.domain.User;

@Service
public class AmbariClusterService {

    public void createCluster(User user, Long cloudId, ClusterJson clusterRequest) {
        // TODO: get server and port from cloudService
        AmbariClient ambariClient = new AmbariClient("localhost", "49163");
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
    }

    public List<ClusterJson> retrieveClusters(User user, Long cloudId) {
        // TODO: get server and port from cloudService
        AmbariClient ambariClient = new AmbariClient("localhost", "49163");
        System.out.println(ambariClient.getClusters());
        return null;
    }

    public ClusterJson retrieveCluster(User user, Long cloudId, String id) {
        // TODO: get server and port from cloudService
        AmbariClient ambariClient = new AmbariClient("localhost", "49163");
        return null;
    }
}
