package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine.DEFAULT_RECIPES;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.api.client.util.Joiner;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.recipe.CentralRecipeUpdater;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeExecutionFailureCollector.RecipeExecutionFailure;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.recipe.GeneratedRecipeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.vault.VaultService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
class OrchestratorRecipeExecutor {

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

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private GeneratedRecipeService generatedRecipeService;

    @Inject
    private RecipeExecutionFailureCollector recipeExecutionFailureCollector;

    @Inject
    private CentralRecipeUpdater centralRecipeUpdater;

    @Inject
    private VaultService vaultService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public void uploadRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        Map<HostGroup, List<RecipeModel>> recipeMap = getHostgroupToRecipeMap(stack, hostGroups);
        Map<String, List<RecipeModel>> hostnameToRecipeMap = recipeMap.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getName(), Entry::getValue));
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        recipesEvent(stack.getId(), stack.getStatus(), hostnameToRecipeMap);
        try {
            hostOrchestrator.uploadRecipes(allGatewayConfigs, hostnameToRecipeMap, clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void preAmbariStartRecipes(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.preAmbariStartRecipes(gatewayConfig, stackUtil.collectNodes(stack),
                    clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = getRecipeExecutionFaiureMessage(stack, e);
            throw new CloudbreakException(message);
        }
    }

    public void postAmbariStartRecipes(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.postAmbariStartRecipes(gatewayConfig, stackUtil.collectNodes(stack),
                    clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = getRecipeExecutionFaiureMessage(stack, e);
            throw new CloudbreakException(message, e);
        }
    }

    public void postClusterInstall(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            hostOrchestrator.postInstallRecipes(gatewayConfig, stackUtil.collectNodes(stack),
                    clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = getRecipeExecutionFaiureMessage(stack, e);
            throw new CloudbreakException(message, e);
        }
    }

    public void preTerminationRecipes(Stack stack) throws CloudbreakException {
        preTerminationRecipesOnNodes(stack, stackUtil.collectNodes(stack));
    }

    public void preTerminationRecipes(Stack stack, Set<String> hostNames) throws CloudbreakException {
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
            String message = getRecipeExecutionFaiureMessage(stack, e);
            throw new CloudbreakException(message, e);
        }
    }

    private String getRecipeExecutionFaiureMessage(Stack stack, CloudbreakOrchestratorException exception) {
        if (!recipeExecutionFailureCollector.canProcessExecutionFailure(exception)) {
            return exception.getMessage();
        }
        Map<HostGroup, List<RecipeModel>> recipeMap = getHostgroupToRecipeMap(stack, hostGroupService.getByCluster(stack.getCluster().getId()));
        Set<RecipeExecutionFailure> failures = recipeExecutionFailureCollector.collectErrors(exception, recipeMap,
                instanceGroupService.findByStackId(stack.getId()));
        String message = failures.stream().map(failure -> new StringBuilder("[Recipe: '")
                .append(failure.getRecipe().getName())
                .append("' - \n")
                .append("Hostgroup: '")
                .append(failure.getInstanceMetaData().getInstanceGroup().getGroupName())
                .append("' - \n")
                .append("Instance: '")
                .append(failure.getInstanceMetaData().getDiscoveryFQDN())
                .append(']')
                .toString()).collect(Collectors.joining(" ---------------------------------------------- ")
        );
        return new StringBuilder("Failed to execute recipe(s): \n").append(message).toString();
    }

    private Map<HostGroup, List<RecipeModel>> getHostgroupToRecipeMap(Stack stack, Set<HostGroup> hostGroups) {
        Map<HostGroup, List<RecipeModel>> recipeModels = hostGroups.stream().filter(hg -> !hg.getRecipes().isEmpty())
                .collect(Collectors.toMap(h -> h, h -> convert(stack, h.getRecipes())));
        prepareGeneratedRecipes(recipeModels);
        return recipeModels;
    }

    private void prepareGeneratedRecipes(Map<HostGroup, List<RecipeModel>> recipeModels) {
        for (Entry<HostGroup, List<RecipeModel>> hostGroupListEntry : recipeModels.entrySet()) {
            for (RecipeModel recipeModel : hostGroupListEntry.getValue()) {
                GeneratedRecipe generatedRecipe = new GeneratedRecipe();
                generatedRecipe.setHostGroup(hostGroupListEntry.getKey());
                generatedRecipe.setExtendedRecipe(recipeModel.getGeneratedScript());
                generatedRecipe.setOriginalRecipe(recipeModel.getScript());
                generatedRecipeService.save(generatedRecipe);
            }
        }
    }

    private List<RecipeModel> convert(Stack stack, Set<Recipe> recipes) {
        List<RecipeModel> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            String fromVault = vaultService.resolveSingleValue(recipe.getContent());
            String decodedContent = new String(Base64.decodeBase64(fromVault));
            TemplatePreparationObject templatePreparationObject = conversionService.convert(stack, TemplatePreparationObject.class);
            String generatedRecipeText = centralRecipeUpdater.getRecipeText(templatePreparationObject, decodedContent);
            RecipeModel recipeModel = new RecipeModel(recipe.getName(), recipe.getRecipeType(), generatedRecipeText, recipe.getContent(), generatedRecipeText);
            result.add(recipeModel);
        }
        return result;
    }

    private Set<Node> collectNodes(Stack stack, Set<String> hostNames) {
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
