package com.sequenceiq.cloudbreak.service.sharedservice;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class SharedServiceConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedServiceConfigProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    public Cluster configureCluster(@Nonnull Cluster requestedCluster, IdentityUser user, ConnectedClusterRequest connectedClusterRequest) {
        Objects.requireNonNull(requestedCluster);
        if (connectedClusterRequest != null) {
            Stack publicStack = queryStack(user, connectedClusterRequest.getSourceClusterId(),
                    Optional.ofNullable(connectedClusterRequest.getSourceClusterName()));
            Cluster sourceCluster = queryCluster(publicStack);
            if (sourceCluster != null) {
                setupLdap(requestedCluster, publicStack);
                setupRds(requestedCluster, sourceCluster);
            }
        }
        return requestedCluster;
    }

    public boolean isConfigured(@Nonnull ClusterV2Request clusterV2Request) {
        return clusterV2Request.getSharedService() != null && !Strings.isNullOrEmpty(clusterV2Request.getSharedService().getSharedCluster());
    }

    public Optional<StackInputs> prepareDatalakeConfigs(Blueprint blueprint, Stack publicStack) {
        try {
            if (publicStack.getDatalakeId() != null) {
                ConfigsResponse configsResponse = retrieveOutputs(stackService.get(publicStack.getDatalakeId()), blueprint, publicStack.getName());
                StackInputs stackInputs = publicStack.getInputs().get(StackInputs.class);
                stackInputs.setDatalakeInputs(configsResponse.getDatalakeInputs());
                stackInputs.setFixInputs(configsResponse.getFixInputs());

                return Optional.ofNullable(stackInputs);
            }
        } catch (IOException e) {
            LOGGER.error("Could not propagate cluster input parameters", e);
            throw new BadRequestException("Could not propagate cluster input parameters: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Set<BlueprintParameterJson> getBlueprintParameterJsons(Blueprint blueprint) throws IOException {
        Set<BlueprintParameterJson> requests = new HashSet<>();
        Json blueprintAttributes = blueprint.getInputParameters();
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
        return requests;
    }

    public Stack updateStackinputs(StackInputs stackInputs, Stack stack) throws BadRequestException {
        try {
            stack.setInputs(new Json(stackInputs));
            stack = stackService.save(stack);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("An error occured under the stackinput persistence which cause a stack creation problem", e);
        }
        return stack;
    }

    public ConfigsResponse retrieveOutputs(Stack datalake, Blueprint blueprint, String stackName) throws IOException {
        AmbariClient ambariClient = ambariClientFactory.getAmbariClient(datalake, datalake.getCluster());

        Set<String> datalakeProperties = centralBlueprintParameterQueryService.queryDatalakeParameters(blueprint.getBlueprintText());
        for (BlueprintParameterJson request : getBlueprintParameterJsons(blueprint)) {
            datalakeProperties.add(request.getReferenceConfiguration());
        }
        addDatalakeRequiredProperties(datalakeProperties);
        Map<String, Object> results = new HashMap<>();
        if (datalake.getAmbariIp() != null) {
            Map<String, String> configs = ambariClient.getConfigValuesByConfigIds(Lists.newArrayList(datalakeProperties));
            results.putAll(configs);

        }
        ConfigsResponse configsResponse = new ConfigsResponse();
        configsResponse.setInputs(new HashSet<>());
        configsResponse.setDatalakeInputs(results);
        configsResponse.setFixInputs(prepareAdditionalInputParameters(datalake.getName(), stackName));
        return configsResponse;
    }

    private void addDatalakeRequiredProperties(Set<String> datalakeProperties) {
        datalakeProperties.add("ranger.audit.solr.zookeepers");
        datalakeProperties.add("ranger_admin_username");
        datalakeProperties.add("policymgr_external_url");
    }

    public Map<String, Object> prepareAdditionalInputParameters(String sourceClustername, String clusterName) {
        Map<String, Object> result = new HashMap<>();
        result.put("REMOTE_CLUSTER_NAME", sourceClustername);
        result.put("remoteClusterName", sourceClustername);
        result.put("remote.cluster.name", sourceClustername);
        result.put("cluster_name", clusterName);
        result.put("cluster.name", clusterName);
        return result;
    }

    private void setupRds(Cluster requestedCluster, Cluster sourceCluster) {
        if (requestedCluster.getRdsConfigs().isEmpty()) {
            requestedCluster.getRdsConfigs().addAll(
                    sourceCluster.getRdsConfigs()
                            .stream()
                            .filter(rdsConfig -> !ResourceStatus.DEFAULT.equals(rdsConfig.getStatus()))
                            .collect(toSet()));
        }
    }

    private Cluster queryCluster(Stack publicStack) {
        return clusterService.getById(publicStack.getCluster().getId());
    }

    private Stack queryStack(IdentityUser user, Long sourceClusterId, Optional<String> sourceClusterName) {
        return sourceClusterName.isPresent() ? stackService.getPublicStack(sourceClusterName.get(), user) : stackService.get(sourceClusterId);
    }

    private void setupLdap(Cluster requestedCluster, Stack publicStack) {
        requestedCluster.setLdapConfig(publicStack.getCluster().getLdapConfig());
    }
}
