package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Component
public class OrchestratorRecipeExecutor {

    private static final String PRE_INSTALL_TAG = "recipe-pre-install:";
    private static final String POST_INSTALL_TAG = "recipe-post-install:";

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;
    @Inject
    private TlsSecurityService tlsSecurityService;

    public void preInstall(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        Map<String, List<RecipeModel>> recipeMap = hostGroups.stream().filter(hg -> !hg.getRecipes().isEmpty())
                .collect(Collectors.toMap(HostGroup::getName, h -> convert(h.getRecipes())));
        InstanceMetaData gatewayInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN());
        try {
            hostOrchestrator.uploadRecipes(gatewayConfig, recipeMap, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
            hostOrchestrator.preInstallRecipes(gatewayConfig, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void postInstall(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        InstanceMetaData gatewayInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN());
        try {
            hostOrchestrator.postInstallRecipes(gatewayConfig, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    private List<RecipeModel> convert(Set<Recipe> recipes) {
        List<RecipeModel> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            recipe.getPlugins().keySet().stream().filter(rawRecipe -> rawRecipe.startsWith("base64://")).forEach(rawRecipe -> {
                String decodedRecipe = new String(Base64.decodeBase64(rawRecipe.replaceFirst("base64://", "")));
                RecipeModel recipeModel = new RecipeModel(recipe.getName());
                if (decodedRecipe.contains(PRE_INSTALL_TAG)) {
                    recipeModel.setPreInstall(
                            new String(Base64.decodeBase64(decodedRecipe.substring(decodedRecipe.indexOf(PRE_INSTALL_TAG) + PRE_INSTALL_TAG.length()))));
                } else if (decodedRecipe.contains("recipe-post-install")) {
                    recipeModel.setPostInstall(
                            new String(Base64.decodeBase64(decodedRecipe.substring(decodedRecipe.indexOf(POST_INSTALL_TAG) + POST_INSTALL_TAG.length()))));
                }
                recipeModel.setKeyValues(recipe.getKeyValues());
                result.add(recipeModel);
            });
        }
        return result;
    }

    private Set<Node> collectNodes(Stack stack) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                Node node = new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryFQDN());
                node.setHostGroup(instanceGroup.getGroupName());
                agents.add(node);
            }
        }
        return agents;
    }
}
