package com.sequenceiq.cloudbreak.service.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;

import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;
import reactor.event.Event;

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
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Autowired
    private HostMetadataRepository hostMetadataRepository;

    @Autowired
    private Reactor reactor;

    @Override
    public void create(CbUser user, Long stackId, Cluster cluster) {
        Stack stack = stackRepository.findOne(stackId);
        LOGGER.info("Cluster requested for stack '{}' [BlueprintId: {}]", stackId, cluster.getBlueprint().getId());
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [stack: '%s', cluster: '%s']", stackId, stack.getCluster()
                    .getName()));
        }
        cluster.setOwner(user.getUsername());
        cluster.setAccount(user.getAccount());
        try {
            cluster = clusterRepository.save(cluster);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(cluster.getName(), ex);
        }
        stack = stackUpdater.updateStackCluster(stack.getId(), cluster);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.CLUSTER_REQUESTED_EVENT, stack.getId());
        reactor.notify(ReactorConfig.CLUSTER_REQUESTED_EVENT, Event.wrap(stack));
    }

    @Override
    public Cluster retrieveCluster(Long stackId) {
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
    public void updateHosts(Long stackId, Set<HostGroupAdjustmentJson> hostGroupAdjustments) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        boolean decommisionRequest = validateRequest(stack, hostGroupAdjustments);
        LOGGER.info("Cluster update requested for stack '{}' [BlueprintId: {}]", stackId, stack.getCluster().getBlueprint().getId());
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.UPDATE_AMBARI_HOSTS_REQUEST_EVENT, stack.getId());
        reactor.notify(ReactorConfig.UPDATE_AMBARI_HOSTS_REQUEST_EVENT, Event.wrap(
                new UpdateAmbariHostsRequest(stackId, hostGroupAdjustments, decommisionRequest)));
    }

    @Override
    public void updateStatus(Long stackId, StatusRequest statusRequest) {
        throw new BadRequestException("Stopping/restarting a cluster is not yet supported");
    }

    @VisibleForTesting
    protected AmbariClient createAmbariClient(String ambariIp) {
        return new AmbariClient(ambariIp, PORT);
    }

    private boolean validateRequest(Stack stack, Set<HostGroupAdjustmentJson> hostGroupAdjustments) {
        int sumScalingAdjustments = 0;
        boolean positive = false;
        boolean negative = false;
        Set<String> hostGroupNames = new HashSet<>();
        for (HostGroupAdjustmentJson hostGroupAdjustment : hostGroupAdjustments) {
            if (hostGroupNames.contains(hostGroupAdjustment.getHostGroup())) {
                throw new BadRequestException(String.format(
                        "Hostgroups cannot be listed more than once in an update request, but '%s' is listed multiple times.",
                        hostGroupAdjustment.getHostGroup()));
            }
            hostGroupNames.add(hostGroupAdjustment.getHostGroup());
            int scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
            if (scalingAdjustment < 0) {
                negative = true;
            } else if (scalingAdjustment > 0) {
                positive = true;
            }
            if (positive && negative) {
                throw new BadRequestException("An update request must contain only decomissions or only additions.");
            }
            sumScalingAdjustments += scalingAdjustment;
        }
        validateZeroScalingAdjustments(sumScalingAdjustments);
        if (!negative) {
            validateUnregisteredHosts(stack, sumScalingAdjustments);
        } else {
            validateRegisteredHosts(stack, hostGroupAdjustments);
        }
        validateHostGroups(stack, hostGroupAdjustments);
        return negative;
    }

    private void validateZeroScalingAdjustments(int sumScalingAdjustments) {
        if (sumScalingAdjustments == 0) {
            throw new BadRequestException("No scaling adjustments specified. Nothing to do.");
        }
    }

    private void validateUnregisteredHosts(Stack stack, int sumScalingAdjustments) {
        Set<InstanceMetaData> unregisteredHosts = instanceMetadataRepository.findUnregisteredHostsInStack(stack.getId());
        if (unregisteredHosts.size() == 0) {
            throw new BadRequestException(String.format(
                    "There are no unregistered hosts in stack '%s'. Add some additional nodes to the stack before adding new hosts to the cluster.",
                    stack.getId()));
        }
        if (unregisteredHosts.size() < sumScalingAdjustments) {
            throw new BadRequestException(String.format("Number of unregistered hosts in the stack is %s, but %s would be needed to complete the request.",
                    unregisteredHosts.size(), sumScalingAdjustments));
        }
    }

    private void validateRegisteredHosts(Stack stack, Set<HostGroupAdjustmentJson> hostGroupAdjustments) {
        List<String> validationErrors = new ArrayList<>();
        for (HostGroupAdjustmentJson hostGroupAdjustment : hostGroupAdjustments) {
            Set<HostMetadata> hostMetadata = hostMetadataRepository.findHostsInHostgroup(hostGroupAdjustment.getHostGroup(), stack.getCluster().getId());
            if (hostMetadata.size() <= -1 * hostGroupAdjustment.getScalingAdjustment()) {
                validationErrors.add(String.format("[hostGroup: '%s', current hosts: %s, decommisions requested: %s]",
                        hostGroupAdjustment.getHostGroup(), hostMetadata.size(), -1 * hostGroupAdjustment.getScalingAdjustment()));
            }
        }
        if (validationErrors.size() > 0) {
            throw new BadRequestException(String.format(
                    "Every host group must contain at least 1 host after the decommision: %s",
                    validationErrors));
        }
    }

    private void validateHostGroups(Stack stack, Set<HostGroupAdjustmentJson> hostGroupAdjustments) {
        for (HostGroupAdjustmentJson hostGroupAdjustment : hostGroupAdjustments) {
            if (!assignableHostgroup(stack.getCluster(), hostGroupAdjustment.getHostGroup())) {
                throw new BadRequestException(String.format(
                        "Invalid hostgroup: blueprint '%s' that was used to create the cluster does not contain a hostgroup with this name.",
                        stack.getCluster().getBlueprint().getId(), hostGroupAdjustment.getHostGroup()));
            }
        }
    }

    private Boolean assignableHostgroup(Cluster cluster, String hostgroup) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root;
            root = mapper.readTree(cluster.getBlueprint().getBlueprintText());
            Iterator<JsonNode> hostGroupsIterator = root.path("host_groups").elements();
            while (hostGroupsIterator.hasNext()) {
                JsonNode hostGroup = hostGroupsIterator.next();
                if (hostGroup.path("name").asText().equals(hostgroup)) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new InternalServerException("Unhandled exception occured while reading blueprint: " + e.getMessage(), e);
        }
        return false;
    }
}
