package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPLOAD_RECIPES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.util.Joiner;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeExecutionFailureCollector.RecipeFailure;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
class OrchestratorRecipeExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private RecipeExecutionFailureCollector recipeExecutionFailureCollector;

    public void uploadRecipes(Stack stack, Map<HostGroup, List<RecipeModel>> recipeModels) throws CloudbreakException {
        Map<String, List<RecipeModel>> hostnameToRecipeMap = recipeModels.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getName(), Entry::getValue));
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        recipesEvent(stack.getId(), stack.getStatus(), hostnameToRecipeMap);
        try {
            hostOrchestrator.uploadRecipes(allGatewayConfigs, hostnameToRecipeMap, clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void preClusterManagerStartRecipes(Stack stack) throws CloudbreakException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.preClusterManagerStartRecipes(gatewayConfig, stackUtil.collectReachableNodes(stack),
                    clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorTimeoutException timeoutException) {
            String preClusterManagerStartException = "Pre cluster manager start" + getRecipeTimeoutErrorMessage(timeoutException);
            LOGGER.info("{} {}", preClusterManagerStartException, timeoutException);
            throw new CloudbreakException(preClusterManagerStartException, timeoutException);
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = getRecipeExecutionFailureMessage(stack, e);
            LOGGER.info(message);
            throw new CloudbreakException(message);
        }
    }

    public void postClusterManagerStartRecipes(Stack stack) throws CloudbreakException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.postClusterManagerStartRecipes(gatewayConfig, stackUtil.collectReachableNodes(stack),
                    clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorTimeoutException timeoutException) {
            String postClusterManagerStartException = "Post cluster manager start" + getRecipeTimeoutErrorMessage(timeoutException);
            LOGGER.info("{} {}", postClusterManagerStartException, timeoutException);
            throw new CloudbreakException(postClusterManagerStartException, timeoutException);
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = getRecipeExecutionFailureMessage(stack, e);
            LOGGER.info(message);
            throw new CloudbreakException(message, e);
        }
    }

    public void postClusterInstall(Stack stack) throws CloudbreakException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.postInstallRecipes(gatewayConfig, stackUtil.collectReachableNodes(stack),
                    clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorTimeoutException timeoutException) {
            String postInstallException = getRecipeTimeoutErrorMessage(timeoutException);
            LOGGER.info("{} {}", postInstallException, timeoutException);
            throw new CloudbreakException(postInstallException, timeoutException);
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = getRecipeExecutionFailureMessage(stack, e);
            LOGGER.info(message);
            throw new CloudbreakException(message, e);
        }
    }

    public void preTerminationRecipes(Stack stack, boolean forced) throws CloudbreakException {
        preTerminationRecipesOnNodes(stack, stackUtil.collectReachableNodes(stack), forced);
    }

    public void preTerminationRecipes(Stack stack, Set<String> hostNames) throws CloudbreakException {
        preTerminationRecipesOnNodes(stack, collectNodes(stack, hostNames), false);
    }

    public void preTerminationRecipesOnNodes(Stack stack, Set<Node> nodes, boolean forced) throws CloudbreakException {
        if (stack.getCluster() == null) {
            throw new NotFoundException("Cluster does not found, pre-termination will not be run.");
        }
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.preTerminationRecipes(gatewayConfig, nodes, ClusterDeletionBasedExitCriteriaModel.nonCancellableModel(), forced);
        } catch (CloudbreakOrchestratorTimeoutException timeoutException) {
            String preTerminationException = "Pre-termination" + getRecipeTimeoutErrorMessage(timeoutException);
            LOGGER.info("{} {}", preTerminationException, timeoutException);
            throw new CloudbreakException(preTerminationException, timeoutException);
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = getRecipeExecutionFailureMessage(stack, e);
            LOGGER.info(message);
            throw new CloudbreakException(message, e);
        }
    }

    private String getRecipeTimeoutErrorMessage(CloudbreakOrchestratorTimeoutException timeoutException) {
        return " recipe(s) failed to finish in " + timeoutException.getTimeoutMinutes() +
                " minute(s), please check your recipe(s) and recipe logs on the machines under /var/log/recipes! Reason:" + timeoutException.getMessage();
    }

    private String getRecipeExecutionFailureMessage(Stack stack, CloudbreakOrchestratorException exception) {
        LOGGER.info("Getting execution failure message in stack {} for exception", stack.getId(), exception);
        if (!recipeExecutionFailureCollector.canProcessExecutionFailure(exception)) {
            return exception.getMessage();
        }
        List<RecipeFailure> failures = recipeExecutionFailureCollector.collectErrors(exception);
        Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.getAllInstanceMetadataByStackId(stack.getId());

        String message = failures.stream()
                .map(failure -> getSingleRecipeExecutionFailureMessage(instanceMetaData, failure))
                .collect(Collectors.joining("\n ---------------------------------------------- \n"));
        return new StringBuilder("Failed to execute recipe(s): \n").append(message).toString();
    }

    @VisibleForTesting
    String getSingleRecipeExecutionFailureMessage(Set<InstanceMetaData> instanceMetaData, RecipeFailure failure) {
        String host = recipeExecutionFailureCollector.getInstanceMetadataByHost(instanceMetaData, failure.getHost())
                .map(metadata -> new StringBuilder("Hostgroup: '")
                        .append(metadata.getInstanceGroup().getGroupName())
                        .append("' - \n")
                        .append("Instance: '")
                        .append(metadata.getDiscoveryFQDN())
                        .append("'")
                        .toString())
                .orElse(new StringBuilder("Instance: '")
                        .append(failure.getHost())
                        .append("' (missing metadata)")
                        .toString());
        return new StringBuilder("[Recipe: '")
                .append(failure.getRecipeName())
                .append("' - \n")
                .append(host)
                .append(']')
                .toString();
    }

    private Set<Node> collectNodes(Stack stack, Set<String> hostNames) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                if (hostNames.contains(im.getDiscoveryFQDN())) {
                    String instanceId = im.getInstanceId();
                    String instanceType = instanceGroup.getTemplate().getInstanceType();
                    agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                            im.getDiscoveryFQDN(), im.getInstanceGroupName()));
                }
            }
        }
        return agents;
    }

    private void recipesEvent(Long stackId, Status status, Map<String, List<RecipeModel>> recipeMap) {
        List<String> recipes = new ArrayList<>();
        for (Entry<String, List<RecipeModel>> entry : recipeMap.entrySet()) {
            Collection<String> recipeNamesPerHostgroup = new ArrayList<>(entry.getValue().size());
            for (RecipeModel rm : entry.getValue()) {
                recipeNamesPerHostgroup.add(rm.getName());
            }
            if (!recipeNamesPerHostgroup.isEmpty()) {
                String recipeNamesStr = Joiner.on(',').join(recipeNamesPerHostgroup);
                recipes.add(String.format("%s:[%s]", entry.getKey(), recipeNamesStr));
            }
        }

        if (!recipes.isEmpty()) {
            Collections.sort(recipes);
            String messageStr = Joiner.on(';').join(recipes);
            cloudbreakEventService.fireCloudbreakEvent(stackId, status.name(), CLUSTER_UPLOAD_RECIPES, Collections.singletonList(messageStr));
        }
    }
}
