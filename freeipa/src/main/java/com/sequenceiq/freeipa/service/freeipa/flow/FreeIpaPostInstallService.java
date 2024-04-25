package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.Permission;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.config.SidGenerationConfigurator;
import com.sequenceiq.freeipa.service.freeipa.host.MaxHostnameLengthPolicyService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaPostInstallService {
    public static final String USER_ADMIN_PRIVILEGE = "User Administrators";

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPostInstallService.class);

    private static final String SET_PASSWORD_EXPIRATION_PERMISSION = "Set Password Expiration";

    private static final int MAX_NAME_LENGTH = 255;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private UserSyncService userSyncService;

    @Inject
    private StackService stackService;

    @Inject
    private PasswordPolicyService passwordPolicyService;

    @Inject
    private FreeIpaPermissionService freeIpaPermissionService;

    @Inject
    private FreeIpaTopologyService freeIpaTopologyService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    @Inject
    private UserSyncBindUserService userSyncBindUserService;

    @Inject
    private MaxHostnameLengthPolicyService hostnameLengthPolicyService;

    @Inject
    private SidGenerationConfigurator sidGenerationConfigurator;

    @Retryable(value = FreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void postInstallFreeIpa(Long stackId, boolean fullPostInstall) throws Exception {
        LOGGER.debug("Performing post-install configuration for stack {}. {}.", stackId, fullPostInstall ? "Full post install" : "Partial post install");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        freeIpaTopologyService.updateReplicationTopology(stackId, Set.of(), freeIpaClient);
        hostnameLengthPolicyService.updateMaxHostnameLength(stack, freeIpaClient);
        Benchmark.measure(() -> sidGenerationConfigurator.enableAndTriggerSidGeneration(stack, freeIpaClient), LOGGER, "Post-install configuration took {}ms.");
        if (fullPostInstall) {
            setInitialFreeIpaPolicies(stack, freeIpaClient);
            userSyncBindUserService.createUserAndLdapConfig(stack, freeIpaClient);
            synchronizeUsers(stack);
        } else {
            if (!userSyncBindUserService.doesBindUserAndConfigAlreadyExist(stack, freeIpaClient)) {
                userSyncBindUserService.createUserAndLdapConfig(stack, freeIpaClient);
            } else {
                LOGGER.debug("Usersync bind user already exists, ensure it's part of the admin group.");
                userSyncBindUserService.addBindUserToAdminGroup(stack, freeIpaClient);
            }
        }
        if (freeIpaRecipeService.hasRecipeType(stackId, RecipeType.POST_SERVICE_DEPLOYMENT, RecipeType.POST_CLUSTER_INSTALL)) {
            executePostServiceDeploymentRecipes(stack);
        } else {
            LOGGER.info("We have no post-install recipes for this stack");
        }
    }

    private void executePostServiceDeploymentRecipes(Stack stack) throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
        LOGGER.info("Execute post install recipes for freeipa: {}. Instances: {}", stack.getName(),
                instanceMetaDatas.stream().map(InstanceMetaData::getInstanceId).collect(Collectors.joining()));
        hostOrchestrator.postServiceDeploymentRecipes(primaryGatewayConfig, allNodes, new StackBasedExitCriteriaModel(stack.getId()));
    }

    private void setInitialFreeIpaPolicies(Stack stack, FreeIpaClient freeIpaClient) throws Exception {
        Set<Permission> permission = freeIpaClient.findPermission(SET_PASSWORD_EXPIRATION_PERMISSION);
        if (permission.isEmpty()) {
            freeIpaClient.addPasswordExpirationPermission(SET_PASSWORD_EXPIRATION_PERMISSION);
        }
        freeIpaClient.addPermissionToPrivilege(USER_ADMIN_PRIVILEGE, SET_PASSWORD_EXPIRATION_PERMISSION);
        freeIpaPermissionService.setPermissions(stack, freeIpaClient);
        Config ipaConfig = freeIpaClient.getConfig();
        updateMaxUsernameLength(freeIpaClient, ipaConfig);
        passwordPolicyService.updatePasswordPolicy(freeIpaClient);
        modifyAdminPasswordExpirationIfNeeded(freeIpaClient);
    }

    private void updateMaxUsernameLength(FreeIpaClient freeIpaClient, Config ipaConfig) throws FreeIpaClientException {
        if (!Objects.equals(MAX_NAME_LENGTH, ipaConfig.getIpamaxusernamelength())) {
            LOGGER.debug("Set maximum username length to {}", MAX_NAME_LENGTH);
            freeIpaClient.setUsernameLength(MAX_NAME_LENGTH);
        }
    }

    private void synchronizeUsers(Stack stack) {
        if (entitlementService.isWorkloadIamSyncEnabled(stack.getAccountId())) {
            LOGGER.debug("WORKLOAD_IAM_SYNC entitled. Usersync will be triggered automatically by the Workload IAM service");
        } else if (entitlementService.isWiamUsersyncRoutingEnabled(stack.getAccountId())) {
            LOGGER.debug("Initiating usersync via WIAM will be started later in {}", FreeIpaProvisionState.FREEIPA_PROVISION_FINISHED_STATE);
        } else {
            LOGGER.debug("WORKLOAD_IAM_SYNC not entitled. Explicitly triggering initial usersync.");
            userSyncService.synchronizeUsers(
                    ThreadBasedUserCrnProvider.getAccountId(), ThreadBasedUserCrnProvider.getUserCrn(), Set.of(stack.getEnvironmentCrn()),
                    Set.of(), Set.of(), WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED);
        }
    }

    private void modifyAdminPasswordExpirationIfNeeded(FreeIpaClient client) throws FreeIpaClientException {
        Optional<User> user = client.userFind(freeIpaClientFactory.getAdminUser());
        if (user.isPresent() && !FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME.equals(user.get().getKrbPasswordExpiration())) {
            User actualUser = user.get();
            LOGGER.debug(String.format("Modifying user [%s] current password expiration time [%s] to [%s]",
                    actualUser.getUid(), actualUser.getKrbPasswordExpiration(), FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME));
            client.updateUserPasswordMaxExpiration(actualUser.getUid());
        } else if (user.isEmpty()) {
            LOGGER.warn(String.format("No [%s] user found!", freeIpaClientFactory.getAdminUser()));
        } else {
            LOGGER.debug("Password expiration is already set.");
        }
    }

}
