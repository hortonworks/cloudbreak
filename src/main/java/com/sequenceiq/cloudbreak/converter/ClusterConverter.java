package com.sequenceiq.cloudbreak.converter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
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
    private RecipeRepository recipeRepository;

    @Autowired
    private InstanceGroupRepository instanceGroupRepository;

    @Autowired
    private HostGroupRepository hostGroupRepository;

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private BlueprintValidator blueprintValidator;

    public Cluster convert(ClusterRequest clusterRequest) {
        return convert(clusterRequest, null);
    }

    public Cluster convert(ClusterRequest clusterRequest, Long stackId) {
        if (stackId == null) {
            throw new BadRequestException("Stack id can not be null");
        }
        Cluster cluster = new Cluster();
        try {
            Blueprint blueprint = blueprintRepository.findOne(clusterRequest.getBlueprintId());
            cluster.setBlueprint(blueprint);
            cluster.setHostGroups(convertHostGroupsFromJson(stackId, cluster, clusterRequest.getHostGroups()));
            blueprintValidator.validateBlueprintForStack(blueprint, cluster.getHostGroups(), stackRepository.findOne(stackId).getInstanceGroups());
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", clusterRequest.getBlueprintId()), e);
        }
        if (clusterRequest.getRecipeId() != null) {
            try {
                Recipe recipe = recipeRepository.findOne(clusterRequest.getRecipeId());
                cluster.setRecipe(recipe);
            } catch (AccessDeniedException e) {
                throw new AccessDeniedException(String.format("Access to recipe '%s' is denied or recipe doesn't exist.", clusterRequest.getRecipeId()), e);
            }
        }
        cluster.setName(clusterRequest.getName());
        cluster.setStatus(Status.REQUESTED);
        cluster.setDescription(clusterRequest.getDescription());
        cluster.setEmailNeeded(clusterRequest.getEmailNeeded());
        return cluster;
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
        if (cluster.getBlueprint() == null) {
            clusterResponse.setBlueprintId(null);
        } else {
            clusterResponse.setBlueprintId(cluster.getBlueprint().getId());
        }
        if (cluster.getRecipe() != null) {
            clusterResponse.setRecipeId(cluster.getRecipe().getId());
        }
        clusterResponse.setDescription(cluster.getDescription() == null ? "" : cluster.getDescription());
        clusterResponse.setHostGroups(convertHostGroupsToJson(hostGroupRepository.findHostGroupsInCluster(cluster.getId())));
        return clusterResponse;
    }

    private Set<HostGroup> convertHostGroupsFromJson(Long stackId, Cluster cluster, Set<HostGroupJson> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupJson json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, json.getInstanceGroupName());
            if (instanceGroup == null) {
                throw new BadRequestException(String.format("Cannot find instance group named '%s' in stack '%s'", json.getInstanceGroupName(), stackId));
            }
            hostGroup.setInstanceGroup(instanceGroup);
            hostGroup.setCluster(cluster);
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

    private Set<HostGroupJson> convertHostGroupsToJson(Set<HostGroup> hostGroups) {
        Set<HostGroupJson> hostGroupJsons = new HashSet<>();
        for (HostGroup hostGroup : hostGroups) {
            HostGroupJson hostGroupJson = new HostGroupJson();
            hostGroupJson.setName(hostGroup.getName());
            hostGroupJson.setInstanceGroupName(hostGroup.getInstanceGroup().getGroupName());
            hostGroupJsons.add(hostGroupJson);
        }
        return hostGroupJsons;
    }

}
