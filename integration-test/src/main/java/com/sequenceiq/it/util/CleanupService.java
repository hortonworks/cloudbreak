package com.sequenceiq.it.util;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
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

    public synchronized void deleteTestStacksAndResources(CloudbreakClient cloudbreakClient) throws Exception {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        Set<StackResponse> stacks = cloudbreakClient.stackEndpoint().getPrivates();
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
        Set<NetworkJson> networks = cloudbreakClient.networkEndpoint().getPrivates();
        for (NetworkJson network : networks) {
            if (network.getName().startsWith("it-")) {
                deleteNetwork(cloudbreakClient, String.valueOf(network.getId()));
            }
        }
        Set<SecurityGroupJson> secgroups = cloudbreakClient.securityGroupEndpoint().getPrivates();
        for (SecurityGroupJson secgroup : secgroups) {
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
            if (("AZURE_RM".equals(credential.getCloudPlatform()) && credential.getName().startsWith("its"))
                    || (!"AZURE_RM".equals(credential.getCloudPlatform()) && credential.getName().startsWith("its-"))) {
                deleteCredential(cloudbreakClient, String.valueOf(credential.getId()));
            }
        }
    }

    public boolean deleteCredential(CloudbreakClient cloudbreakClient, String credentialId) throws Exception {
        boolean result = false;
        if (credentialId != null) {
            cloudbreakClient.credentialEndpoint().delete(Long.valueOf(credentialId));
            result = true;
        }
        return result;
    }

    public boolean deleteTemplate(CloudbreakClient cloudbreakClient, String templateId) throws Exception {
        boolean result = false;
        if (templateId != null) {
            cloudbreakClient.templateEndpoint().delete(Long.valueOf(templateId));
            result = true;
        }
        return result;
    }

    public boolean deleteNetwork(CloudbreakClient cloudbreakClient, String networkId) throws Exception {
        boolean result = false;
        if (networkId != null) {
            cloudbreakClient.networkEndpoint().delete(Long.valueOf(networkId));
            result = true;
        }
        return result;
    }

    public boolean deleteSecurityGroup(CloudbreakClient cloudbreakClient, String securityGroupId) throws Exception {
        boolean result = false;
        if (securityGroupId != null) {
            SecurityGroupEndpoint securityGroupEndpoint = cloudbreakClient.securityGroupEndpoint();
            SecurityGroupJson securityGroupJson = securityGroupEndpoint.get(Long.valueOf(securityGroupId));
            if (!securityGroupJson.getName().equals(itProps.getDefaultSecurityGroup())) {
                securityGroupEndpoint.delete(Long.valueOf(securityGroupId));
                result = true;
            }
        }
        return result;
    }

    public boolean deleteBlueprint(CloudbreakClient cloudbreakClient, String blueprintId) throws Exception {
        boolean result = false;
        if (blueprintId != null) {
            cloudbreakClient.blueprintEndpoint().delete(Long.valueOf(blueprintId));
            result = true;
        }
        return result;
    }

    public boolean deleteStackAndWait(CloudbreakClient cloudbreakClient, String stackId) throws Exception {
        boolean deleted = false;
        for (int i = 0; i < cleanUpRetryCount; i++) {
            if (deleteStack(cloudbreakClient, stackId)) {
                WaitResult waitResult = CloudbreakUtil.waitForStackStatus(cloudbreakClient, stackId, "DELETE_COMPLETED");
                if (waitResult == WaitResult.SUCCESSFUL) {
                    deleted = true;
                    break;
                }
                try {
                    Thread.sleep(DELETE_SLEEP);
                } catch (InterruptedException e) {
                    LOG.warn("interrupted ex", e);
                }
            }
        }
        return deleted;
    }

    public boolean deleteStack(CloudbreakClient cloudbreakClient, String stackId) throws Exception {
        boolean result = false;
        if (stackId != null) {
            cloudbreakClient.stackEndpoint().delete(Long.valueOf(stackId), false);
            result = true;
        }
        return result;
    }

    public boolean deleteRecipe(CloudbreakClient cloudbreakClient, Long recipeId) throws Exception {
        cloudbreakClient.recipeEndpoint().delete(recipeId);
        return true;
    }
}
