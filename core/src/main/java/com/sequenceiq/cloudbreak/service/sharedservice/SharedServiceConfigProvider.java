package com.sequenceiq.cloudbreak.service.sharedservice;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class SharedServiceConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedServiceConfigProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    public Cluster configureCluster(Cluster requestedCluster, IdentityUser user, ConnectedClusterRequest connectedClusterRequest) {
        if (connectedClusterRequest != null) {
            Stack publicStack = queryStack(user, connectedClusterRequest.getSourceClusterId(),
                    Optional.ofNullable(connectedClusterRequest.getSourceClusterName()));
            Cluster sourceCluster = queryCluster(publicStack);
            setupLdap(requestedCluster, publicStack);
            setupRds(requestedCluster, sourceCluster);
            setupAdditionalParameters(requestedCluster, publicStack);
        }
        return requestedCluster;
    }

    public Stack configureStack(Stack requestedStack, IdentityUser user) {
        if (requestedStack.getDatalakeId() != null) {
            Stack sourceStack = queryStack(user, requestedStack.getDatalakeId(), null);
            requestedStack.setRegion(sourceStack.getRegion());
            requestedStack.setAvailabilityZone(sourceStack.getAvailabilityZone());
            requestedStack.getNetwork().setAttributes(sourceStack.getNetwork().getAttributes());

        }
        return requestedStack;
    }

    private void setupAdditionalParameters(Cluster requestedCluster, Stack publicStack) {
        try {

            Set<BlueprintParameterJson> requests = new HashSet<>();
            Json blueprintAttributes = requestedCluster.getBlueprint().getInputParameters();
            if (blueprintAttributes != null && StringUtils.isNoneEmpty(blueprintAttributes.getValue())) {
                BlueprintInputParameters inputParametersObj = blueprintAttributes.get(BlueprintInputParameters.class);
                for (BlueprintParameter blueprintParameter : inputParametersObj.getParameters()) {
                    BlueprintParameterJson blueprintParameterJson = new BlueprintParameterJson();
                    blueprintParameterJson.setName(blueprintParameter.getName());
                    blueprintParameterJson.setReferenceConfiguration(blueprintParameter.getReferenceConfiguration());
                    blueprintParameterJson.setDescription(blueprintParameter.getDescription());
                    requests.add(blueprintParameterJson);
                }
            }
            ConfigsResponse configsResponse = clusterService.retrieveOutputs(publicStack.getId(), requests);

            Map<String, String> newInputs = requestedCluster.getBlueprintInputs().get(Map.class);
            for (BlueprintInputJson blueprintInputJson : configsResponse.getInputs()) {
                newInputs.put(blueprintInputJson.getName(), blueprintInputJson.getPropertyValue());
            }
            requestedCluster.setBlueprintInputs(new Json(newInputs));
        } catch (IOException e) {
            LOGGER.error("Could not propagate cluster input parameters", e);
            throw new BadRequestException("Could not propagate cluster input parameters: " + e.getMessage());
        }
    }

    private void setupRds(Cluster requestedCluster, Cluster sourceCluster) {
        requestedCluster.getRdsConfigs().addAll(
                sourceCluster.getRdsConfigs()
                        .stream()
                        .filter(rdsConfig -> !ResourceStatus.DEFAULT.equals(rdsConfig.getStatus()))
                        .collect(toSet()));
    }

    private Cluster queryCluster(Stack publicStack) {
        return clusterService.getById(publicStack.getId());
    }

    private Stack queryStack(IdentityUser user, Long sourceClusterId, Optional<String> sourceClusterName) {
        Stack publicStack;
        if (sourceClusterName.isPresent()) {
            publicStack = stackService.getPublicStack(sourceClusterName.get(), user);
        } else {
            publicStack = stackService.get(sourceClusterId);
        }
        return publicStack;
    }

    private void setupLdap(Cluster requestedCluster, Stack publicStack) {
        requestedCluster.setLdapConfig(publicStack.getCluster().getLdapConfig());
    }


    public boolean configured(ClusterV2Request clusterV2Request) {
        return clusterV2Request.getSharedService() != null && !Strings.isNullOrEmpty(clusterV2Request.getSharedService().getSharedCluster());
    }
}
