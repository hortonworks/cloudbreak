package com.sequenceiq.it.util;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.config.ITProps;

@Component
public class CleanupService {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupService.class);

    private static final int DELETE_SLEEP = 30000;

    private boolean cleanedUp;

    @Value("${integrationtest.cleanup.retryCount}")
    private int cleanUpRetryCount;

    @Inject
    private ITProps itProps;

    public synchronized void deleteTestStacksAndResources(CloudbreakClient cloudbreakClient) {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        Set<StackResponse> stacks = cloudbreakClient.stackV1Endpoint().getPrivates();
        for (StackResponse stack : stacks) {
            if (stack.getName().startsWith("it-")) {
                deleteStackAndWait(cloudbreakClient, String.valueOf(stack.getId()));
            }
        }
        Set<TemplateResponse> templates = cloudbreakClient.templateEndpoint().getPrivates();
        for (TemplateResponse template : templates) {
            if (template.getName().startsWith("it-")) {
                deleteTemplate(cloudbreakClient, String.valueOf(template.getId()));
            }
        }
        Set<NetworkResponse> networks = cloudbreakClient.networkEndpoint().getPrivates();
        for (NetworkResponse network : networks) {
            if (network.getName().startsWith("it-")) {
                deleteNetwork(cloudbreakClient, String.valueOf(network.getId()));
            }
        }
        Set<SecurityGroupResponse> secgroups = cloudbreakClient.securityGroupEndpoint().getPrivates();
        for (SecurityGroupResponse secgroup : secgroups) {
            if (secgroup.getName().startsWith("it-")) {
                deleteSecurityGroup(cloudbreakClient, String.valueOf(secgroup.getId()));
            }
        }
        Set<BlueprintResponse> blueprints = cloudbreakClient.blueprintEndpoint().getPrivates();
        for (BlueprintResponse blueprint : blueprints) {
            if (blueprint.getName().startsWith("it-")) {
                deleteBlueprint(cloudbreakClient, String.valueOf(blueprint.getId()));
            }
        }
        Set<RecipeResponse> recipes = cloudbreakClient.recipeEndpoint().getPrivates();
        for (RecipeResponse recipe : recipes) {
            if (recipe.getName().startsWith("it-")) {
                deleteRecipe(cloudbreakClient, recipe.getId());
            }
        }
        Set<CredentialResponse> credentials = cloudbreakClient.credentialEndpoint().getPrivates();
        for (CredentialResponse credential : credentials) {
            if ("AZURE".equals(credential.getCloudPlatform()) ? credential.getName().startsWith("its") : credential.getName().startsWith("its-")) {
                deleteCredential(cloudbreakClient, String.valueOf(credential.getId()));
            }
        }

        Set<RDSConfigResponse> rdsconfigs = cloudbreakClient.rdsConfigEndpoint().getPrivates();
        for (RDSConfigResponse rds : rdsconfigs) {
            if (rds.getName().startsWith("it-")) {
                deleteRdsConfigs(cloudbreakClient, rds.getId().toString());
            }
        }
    }

    public void deleteCredential(CloudbreakClient cloudbreakClient, String credentialId) {
        if (credentialId != null) {
            cloudbreakClient.credentialEndpoint().delete(Long.valueOf(credentialId));
        }
    }

    public void deleteTemplate(CloudbreakClient cloudbreakClient, String templateId) {
        if (templateId != null) {
            cloudbreakClient.templateEndpoint().delete(Long.valueOf(templateId));
        }
    }

    public void deleteNetwork(CloudbreakClient cloudbreakClient, String networkId) {
        if (networkId != null) {
            cloudbreakClient.networkEndpoint().delete(Long.valueOf(networkId));
        }
    }

    public void deleteSecurityGroup(CloudbreakClient cloudbreakClient, String securityGroupId) {
        if (securityGroupId != null) {
            SecurityGroupEndpoint securityGroupEndpoint = cloudbreakClient.securityGroupEndpoint();
            SecurityGroupResponse securityGroupResponse = securityGroupEndpoint.get(Long.valueOf(securityGroupId));
            if (!itProps.isDefaultSecurityGroup(securityGroupResponse.getName())) {
                securityGroupEndpoint.delete(Long.valueOf(securityGroupId));
            }
        }
    }

    public void deleteBlueprint(CloudbreakClient cloudbreakClient, String blueprintId) {
        if (blueprintId != null) {
            cloudbreakClient.blueprintEndpoint().delete(Long.valueOf(blueprintId));
        }
    }

    public void deleteStackAndWait(CloudbreakClient cloudbreakClient, String stackId) {
        for (int i = 0; i < cleanUpRetryCount; i++) {
            if (deleteStack(cloudbreakClient, stackId)) {
                WaitResult waitResult = CloudbreakUtil.waitForStackStatus(cloudbreakClient, stackId, "DELETE_COMPLETED");
                if (waitResult == WaitResult.SUCCESSFUL) {
                    break;
                }
                try {
                    Thread.sleep(DELETE_SLEEP);
                } catch (InterruptedException e) {
                    LOG.warn("interrupted ex", e);
                }
            }
        }
    }

    public boolean deleteStack(CloudbreakClient cloudbreakClient, String stackId) {
        boolean result = false;
        if (stackId != null) {
            cloudbreakClient.stackV1Endpoint().delete(Long.valueOf(stackId), false, false);
            result = true;
        }
        return result;
    }

    public void deleteRecipe(CloudbreakClient cloudbreakClient, Long recipeId) {
        cloudbreakClient.recipeEndpoint().delete(recipeId);
    }

    public void deleteImageCatalog(CloudbreakClient cloudbreakClient, String name) {
        cloudbreakClient.imageCatalogEndpoint().deletePublic(name);
    }

    public void deleteRdsConfigs(CloudbreakClient cloudbreakClient, String rdsConfigId) {
        if (rdsConfigId != null) {
            cloudbreakClient.rdsConfigEndpoint().delete(Long.valueOf(rdsConfigId));
        }
    }
}
