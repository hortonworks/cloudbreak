package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class DependecyDeletionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependecyDeletionService.class);

    @Inject
    private StackService stackService;

    @Inject
    private NetworkService networkService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private RecipeService recipeService;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private TemplateService templateService;

    @Inject
    private HostGroupService hostGroupService;

    public void deleteDependencies(StackView stackView) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackView.getId());
        deleteDependencies(stack);
    }

    public void deleteDependencies(Stack stack) {
        deleteNetwork(stack.getNetwork());
        deleteCredential(stack.getCredential());

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            deleteSecurityGroup(instanceGroup.getSecurityGroup());
            deleteTemplate(instanceGroup.getTemplate());
        }
        if (stack.getCluster() != null) {
            Cluster cluster = stack.getCluster();
            deleteBlueprint(cluster.getClusterDefinition());
            Set<HostGroup> hostGroupsInCluster = hostGroupService.getByCluster(cluster.getId());
            for (HostGroup hostGroup : hostGroupsInCluster) {
                hostGroup.getRecipes().forEach(this::deleteRecipe);
            }
        }
    }

    private void deleteNetwork(Network network) {
        try {
            if (network != null) {
                networkService.delete(network);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not delete network {} which is associated with the stack: {}", network, ex.getMessage());
        }
    }

    private void deleteCredential(Credential credential) {
        try {
            if (credential != null) {
                credentialService.archiveCredential(credential);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not delete credential {} which is associated with the stack: {}", credential, ex.getMessage());
        }
    }

    private void deleteSecurityGroup(SecurityGroup securityGroup) {
        try {
            if (securityGroup != null) {
                securityGroupService.delete(securityGroup);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not delete securityGroup {} which is associated with the stack: {}", securityGroup, ex.getMessage());
        }
    }

    private void deleteTemplate(Template template) {
        try {
            if (template != null) {
                templateService.delete(template);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not delete template {} which is associated with the stack: {}", template, ex.getMessage());
        }
    }

    private void deleteBlueprint(ClusterDefinition clusterDefinition) {
        try {
            if (clusterDefinition != null) {
                clusterDefinitionService.delete(clusterDefinition);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not delete validation {} which is associated with the stack: {}", clusterDefinition, ex.getMessage());
        }
    }

    private void deleteRecipe(Recipe recipe) {
        try {
            if (recipe != null) {
                recipeService.delete(recipe);
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not delete recipe {} which is associated with the stack: {}", recipe, ex.getMessage());
        }
    }
}
