package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
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
        Map<String, List<RecipeModel>> recipeMap = hostGroups.stream().collect(Collectors.toMap(HostGroup::getName, h -> convert(h.getRecipes())));
        InstanceMetaData gatewayInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN());
        hostOrchestrator.preInstallRecipes(gatewayConfig, recipeMap);
    }

    private List<RecipeModel> convert(Set<Recipe> recipes) {
        List<RecipeModel> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            recipe.getPlugins().keySet().stream().filter(rawRecipe -> rawRecipe.startsWith("base64://")).forEach(rawRecipe -> {
                String decodedRecipe = new String(Base64.decodeBase64(rawRecipe.replaceFirst("base64://", "")));
                RecipeModel recipeModel = new RecipeModel(recipe.getName());
                if (decodedRecipe.contains(PRE_INSTALL_TAG)) {
                    recipeModel.addPreInstall(
                            new String(Base64.decodeBase64(decodedRecipe.substring(decodedRecipe.indexOf(PRE_INSTALL_TAG) + PRE_INSTALL_TAG.length()))));
                } else if (decodedRecipe.contains("recipe-post-install")) {
                    recipeModel.addPostInstall(
                            new String(Base64.decodeBase64(decodedRecipe.substring(decodedRecipe.indexOf(POST_INSTALL_TAG) + POST_INSTALL_TAG.length()))));
                }
                recipeModel.setKeyValues(recipe.getKeyValues());
                result.add(recipeModel);
            });
        }
        return result;
    }
}
