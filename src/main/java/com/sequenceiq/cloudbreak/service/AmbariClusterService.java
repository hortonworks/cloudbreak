package com.sequenceiq.cloudbreak.service;

import groovyx.net.http.HttpResponseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.converter.ClusterConverter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Service
public class AmbariClusterService {

    public static final String PORT = "8080";

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private ClusterConverter clusterConverter;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private AmbariClusterInstaller ambariClusterInstaller;

    public void createCluster(User user, Long stackId, ClusterRequest clusterRequest) {
        Stack stack = stackRepository.findOne(stackId);
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [stack: '%s', cluster: '%s']", stackId, stack.getCluster()
                    .getName()));
        }
        Cluster cluster = clusterConverter.convert(clusterRequest);
        cluster.setUser(user);
        cluster = clusterRepository.save(cluster);

        stack.setCluster(cluster);
        stack = stackRepository.save(stack);
        if (stack.getStatus().equals(Status.CREATE_COMPLETED)) {
            ambariClusterInstaller.installAmbariCluster(stack);
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
            return clusterConverter.convert(stack.getCluster(), clusterJson);
        } catch (HttpResponseException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new NotFoundException("Ambari blueprint not found.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    // public List<BlueprintJson> retrieveBlueprints(User user, Long stackId) {
    // try {
    // List<BlueprintJson> blueprints = new ArrayList<>();
    // Stack stack = stackRepository.findOne(stackId);
    // AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
    // Set<String> blueprintNames = ambariClient.getBlueprintsMap().keySet();
    // for (String blueprintName : blueprintNames) {
    // blueprints.add(createBlueprintJsonFromString(ambariClient.getBlueprintAsJson(blueprintName)));
    // }
    // return blueprints;
    // } catch (IOException e) {
    // throw new
    // InternalServerException("Jackson failed to parse blueprint JSON.", e);
    // }
    // }
    //
    // public BlueprintJson retrieveBlueprint(User user, Long stackId, String
    // id) {
    // Stack stack = stackRepository.findOne(stackId);
    // AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp(), PORT);
    // try {
    // return
    // createBlueprintJsonFromString(ambariClient.getBlueprintAsJson(id));
    // } catch (HttpResponseException e) {
    // if ("Not Found".equals(e.getMessage())) {
    // throw new NotFoundException("Ambari blueprint not found.", e);
    // } else {
    // throw new InternalServerException("Something went wrong", e);
    // }
    // } catch (IOException e) {
    // throw new
    // InternalServerException("Jackson failed to parse blueprint JSON.", e);
    // }
    // }
    //
    // private BlueprintJson createBlueprintJsonFromString(String blueprint)
    // throws IOException {
    // BlueprintJson blueprintJson = new BlueprintJson();
    // blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(blueprint));
    // return blueprintJson;
    // }

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
