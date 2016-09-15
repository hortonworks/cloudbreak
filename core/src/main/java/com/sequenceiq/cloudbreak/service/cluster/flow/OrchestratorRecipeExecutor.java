package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class OrchestratorRecipeExecutor {

    private static final String PRE_INSTALL_TAG = "recipe-pre-install";
    private static final String POST_INSTALL_TAG = "recipe-post-install";

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;
    @Inject
    private GatewayConfigService gatewayConfigService;

    public void uploadRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        Map<String, List<RecipeModel>> recipeMap = hostGroups.stream().filter(hg -> !hg.getRecipes().isEmpty())
                .collect(Collectors.toMap(HostGroup::getName, h -> convert(h.getRecipes())));
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack);
        try {
            hostOrchestrator.uploadRecipes(gatewayConfig, recipeMap, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void preInstall(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack);
        try {
            hostOrchestrator.preInstallRecipes(gatewayConfig, collectNodes(stack),
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakException(e);
        }
    }

    public void postInstall(Stack stack) throws CloudbreakException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack);
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
            recipe.getPlugins().stream().filter(rawRecipe -> rawRecipe.getContent().startsWith("base64://")).forEach(rawRecipe -> {
                String decodedRecipe = new String(Base64.decodeBase64(rawRecipe.getContent().replaceFirst("base64://", "")));
                RecipeModel recipeModel = new RecipeModel(recipe.getName());
                Map<String, String> recipeMap = Stream.of(decodedRecipe.split("\n"))
                        .collect(Collectors.toMap(s -> s.substring(0, s.indexOf(":")), s -> s.substring(s.indexOf(":") + 1)));
                if (recipeMap.containsKey(PRE_INSTALL_TAG)) {
                    recipeModel.setPreInstall(new String(Base64.decodeBase64(recipeMap.get(PRE_INSTALL_TAG))));
                }
                if (recipeMap.containsKey(POST_INSTALL_TAG)) {
                    recipeModel.setPostInstall(new String(Base64.decodeBase64(recipeMap.get(POST_INSTALL_TAG))));
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
