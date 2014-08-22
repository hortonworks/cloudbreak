package com.sequenceiq.cloudbreak.service.cluster;

import groovyx.net.http.HttpResponseException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.Reactor;
import reactor.event.Event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariAddNode;

@Service
public class AmbariClusterService implements ClusterService {

    public static final String PORT = "8080";

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Override
    public void createCluster(User user, Long stackId, Cluster cluster) {
        Stack stack = stackRepository.findOne(stackId);
        LOGGER.info("Cluster requested for stack '{}' [BlueprintId: {}]", stackId, cluster.getBlueprint().getId());
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [stack: '%s', cluster: '%s']", stackId, stack.getCluster()
                    .getName()));
        }
        cluster.setUser(user);
        cluster = clusterRepository.save(cluster);
        stack = stackUpdater.updateStackCluster(stack.getId(), cluster);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.CLUSTER_REQUESTED_EVENT, stack.getId());
        reactor.notify(ReactorConfig.CLUSTER_REQUESTED_EVENT, Event.wrap(stack));
    }

    @Override
    public Cluster retrieveCluster(User user, Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        return stack.getCluster();
    }

    @Override
    public String getClusterJson(String ambariIp, Long stackId) {
        AmbariClient ambariClient = createAmbariClient(ambariIp);
        try {
            String clusterJson = ambariClient.getClusterAsJson();
            if (clusterJson == null) {
                throw new InternalServerException(String.format("Cluster response coming from Ambari server was null. [Stack: '%s', Ambari Server IP: '%s']",
                        stackId, ambariIp));
            }
            return clusterJson;
        } catch (HttpResponseException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new NotFoundException("Ambari blueprint not found.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    @Override
    public void updateHosts(User user, Long stackId, Set<HostGroupAdjustmentJson> hostGroupAdjustments) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        int sumScalingAdjustments = 0;
        for (HostGroupAdjustmentJson hostGroupAdjustment : hostGroupAdjustments) {
            int scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
            if (scalingAdjustment < 0) {
                throw new BadRequestException(String.format("Requested scaling adjustment on hostGroup '%s' is negative (%s), but "
                        + "node decommision is not yet supported by the Cloudbreak API.",
                        hostGroupAdjustment.getHostGroup(), scalingAdjustment));
            }
            sumScalingAdjustments += scalingAdjustment;
        }
        AmbariClient ambariClient = new AmbariClient(stack.getAmbariIp());
        List<String> unregisteredHosts = ambariClient.getUnregisteredHostNames();
        if (unregisteredHosts.size() == 0) {
            throw new BadRequestException(String.format(
                    "There are no unregistered hosts in stack '%s'. Add some additional nodes to the stack before adding new hosts to the cluster.", stackId));
        }
        if (unregisteredHosts.size() < sumScalingAdjustments) {
            throw new BadRequestException(String.format("Number of unregistered hosts in the stack is %s, but %s would be needed to complete the request.",
                    unregisteredHosts.size(), sumScalingAdjustments));
        }
        for (HostGroupAdjustmentJson hostGroupAdjustment : hostGroupAdjustments) {
            try {
                if (!assignableHostgroup(stack.getCluster(), hostGroupAdjustment.getHostGroup())) {
                    throw new BadRequestException(String.format("Invalid hostgroup: blueprint %s does not contain %s hostgroup.",
                            stack.getCluster().getBlueprint().getId(), hostGroupAdjustment.getHostGroup()));
                }
            } catch (Exception e) {
                throw new BadRequestException(String.format("Stack %s put occurs a problem '%s': %s", stackId, e.getMessage(), e));
            }
        }
        LOGGER.info("Cluster update requested for stack '{}' [BlueprintId: {}]", stackId, stack.getCluster().getBlueprint().getId());
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.ADD_NODE_AMBARI_UPDATE_NODE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.ADD_NODE_AMBARI_UPDATE_NODE_EVENT, Event.wrap(
                new AmbariAddNode(stackId, stack.getAmbariIp(), hostGroupAdjustments)));
    }

    private Boolean assignableHostgroup(Cluster cluster, String hostgroup) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(cluster.getBlueprint().getBlueprintText());
        Iterator<JsonNode> hostGroupsIterator = root.path("host_groups").elements();
        while (hostGroupsIterator.hasNext()) {
            JsonNode hostGroup = hostGroupsIterator.next();
            if (hostGroup.path("name").asText().equals(hostgroup)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateStatus(User user, Long stackId, StatusRequest statusRequest) {
        throw new BadRequestException("Stopping/restarting a cluster is not yet supported");
    }

    @VisibleForTesting
    protected AmbariClient createAmbariClient(String ambariIp) {
        return new AmbariClient(ambariIp, PORT);
    }
}
