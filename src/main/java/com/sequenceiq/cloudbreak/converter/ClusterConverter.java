package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class ClusterConverter {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private JsonHelper jsonHelper;

    public Cluster convert(ClusterRequest clusterRequest) {
        return convert(clusterRequest, null);
    }

    public Cluster convert(ClusterRequest clusterRequest, Long stackId) {
        if (stackId == null) {
            throw new BadRequestException("Stack id can not be null");
        }
        Cluster cluster = new Cluster();
        try {
            Blueprint one = blueprintRepository.findOne(clusterRequest.getBlueprintId());
            cluster.setBlueprint(one);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(one.getBlueprintText());
            validateBlueprintRequest(root, stackRepository.findById(stackId));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", clusterRequest.getBlueprintId()), e);
        } catch (IOException e) {
            throw new BadRequestException(String.format("Blueprint [%s] can not convert to json node.", clusterRequest.getBlueprintId()));
        }
        cluster.setName(clusterRequest.getName());
        cluster.setStatus(Status.REQUESTED);
        cluster.setDescription(clusterRequest.getDescription());
        cluster.setEmailNeeded(clusterRequest.getEmailNeeded());
        return cluster;
    }

    private void validateBlueprintRequest(JsonNode root, Stack stack) {
        Iterator<JsonNode> hostGroups = root.get("host_groups").elements();
        int hostGroupCount = 0;
        while (hostGroups.hasNext()) {
            JsonNode next = hostGroups.next();
            String name = next.get("name").asText();
            boolean find = false;
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                if (instanceGroup.getGroupName().equals(name)) {
                    find = true;
                }
            }
            hostGroupCount++;
            if (!find) {
                throw new BadRequestException(String.format("The name of host group '%s' doesn't match any of the defined instance groups.", name));
            }
        }
        if (stack.getInstanceGroups().size() != hostGroupCount) {
            throw new BadRequestException(String.format("Request not defined all instance group on '%s' stack.", stack.getId()));
        }
    }

    public ClusterResponse convert(Cluster cluster, String clusterJson) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setCluster(jsonHelper.createJsonFromString(clusterJson));
        clusterResponse.setId(cluster.getId());
        clusterResponse.setStatus(cluster.getStatus().name());
        if (cluster.getUpSince() != null && Status.AVAILABLE.equals(cluster.getStatus())) {
            long now = new Date().getTime();
            long uptime = now - cluster.getUpSince();
            int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
            int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
            clusterResponse.setHoursUp(hours);
            clusterResponse.setMinutesUp(minutes);
        } else {
            clusterResponse.setHoursUp(0);
            clusterResponse.setMinutesUp(0);
        }
        clusterResponse.setStatusReason(cluster.getStatusReason());
        clusterResponse.setBlueprintId(cluster.getBlueprint().getId());
        clusterResponse.setDescription(cluster.getDescription() == null ? "" : cluster.getDescription());
        return clusterResponse;
    }

}
