package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPES;

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

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.google.api.client.util.Joiner;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class OrchestratorRecipeExecutor {

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private StackUtil stackUtil;

    public void uploadRecipes(Stack stack, Collection<HostGroup> hostGroups) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        Map<String, List<RecipeModel>> recipeMap = hostGroups.stream().filter(hg -> !hg.getRecipes().isEmpty())
                .collect(Collectors.toMap(HostGroup::getName, h -> convert(h.getRecipes())));
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        recipesEvent(stack.getId(), stack.getStatus(), recipeMap);
        try {
            hostOrchestrator.uploadRecipes(allGatewayConfigs, recipeMap, clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void postAmbariStartRecipes(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.postAmbariStartRecipes(gatewayConfig, stackUtil.collectNodes(stack),
                clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void preTerminationRecipes(Stack stack) throws CloudbreakException {
        preTerminationRecipesOnNodes(stack, stackUtil.collectNodes(stack));
    }

    public void preTerminationRecipes(Stack stack, Collection<String> hostNames) throws CloudbreakException {
        preTerminationRecipesOnNodes(stack, collectNodes(stack, hostNames));
    }

    public void preTerminationRecipesOnNodes(Stack stack, Set<Node> nodes) throws CloudbreakException {

        if (stack.getCluster() == null) {
            throw new NotFoundException("Cluster does not found, pre-termination will not be run.");
        }

        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.preTerminationRecipes(gatewayConfig, nodes, clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void postInstall(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.postInstallRecipes(gatewayConfig, stackUtil.collectNodes(stack),
                    clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    private List<RecipeModel> convert(Iterable<Recipe> recipes) {
        List<RecipeModel> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            String decodedContent = new String(Base64.decodeBase64(recipe.getContent()));
            RecipeModel recipeModel = new RecipeModel(recipe.getName(), recipe.getRecipeType(), decodedContent);
            result.add(recipeModel);
        }
        return result;
    }

    private Set<Node> collectNodes(Stack stack, Collection<String> hostNames) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                if (hostNames.contains(im.getDiscoveryFQDN())) {
                    agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getDiscoveryFQDN(), im.getInstanceGroupName()));
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
                //filter out default recipes
                if (!DEFAULT_RECIPES.contains(rm.getName())) {
                    recipeNamesPerHostgroup.add(rm.getName());
                }
            }
            if (!recipeNamesPerHostgroup.isEmpty()) {
                String recipeNamesStr = Joiner.on(',').join(recipeNamesPerHostgroup);
                recipes.add(String.format("%s:[%s]", entry.getKey(), recipeNamesStr));
            }
        }

        if (!recipes.isEmpty()) {
            Collections.sort(recipes);
            String messageStr = Joiner.on(';').join(recipes);
            cloudbreakEventService.fireCloudbreakEvent(stackId, status.name(),
                    cloudbreakMessagesService.getMessage(Msg.UPLOAD_RECIPES.code(), Collections.singletonList(messageStr)));
        }
    }

    private enum Msg {

        UPLOAD_RECIPES("recipes.upload");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}
