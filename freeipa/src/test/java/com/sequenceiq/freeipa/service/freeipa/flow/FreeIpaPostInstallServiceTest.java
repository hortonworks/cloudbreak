package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.config.SidGenerationConfigurator;
import com.sequenceiq.freeipa.service.freeipa.host.MaxHostnameLengthPolicyService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaPostInstallServiceTest {

    private static final String ACCOUNT_ID = "accid";

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", ACCOUNT_ID);

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaTopologyService freeIpaTopologyService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private StackService stackService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private FreeIpaRecipeService freeIpaRecipeService;

    @Mock
    private UserSyncBindUserService userSyncBindUserService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private UserSyncService userSyncService;

    @Mock
    private FreeIpaPermissionService freeIpaPermissionService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private MaxHostnameLengthPolicyService hostnameLengthPolicyService;

    @Mock
    private SidGenerationConfigurator sidGenerationConfigurator;

    @InjectMocks
    private FreeIpaPostInstallService underTest;

    @Test
    public void postInstallFreeIpaExecutePostServiceDeploymentRecipes() throws Exception {
        Stack stack = mock(Stack.class);
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(freeIpaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack)).thenReturn(ipaClient);
        Set<Node> nodes = Set.of(mock(Node.class));
        when(freeIpaNodeUtilService.mapInstancesToNodes(anySet())).thenReturn(nodes);
        when(freeIpaRecipeService.hasRecipeType(1L, RecipeType.POST_SERVICE_DEPLOYMENT, RecipeType.POST_CLUSTER_INSTALL)).thenReturn(true);
        when(userSyncBindUserService.doesBindUserAndConfigAlreadyExist(stack, ipaClient)).thenReturn(false);

        underTest.postInstallFreeIpa(1L, false);

        verify(hostOrchestrator).postServiceDeploymentRecipes(eq(gatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));
        verify(userSyncBindUserService).doesBindUserAndConfigAlreadyExist(stack, ipaClient);
        verify(userSyncBindUserService).createUserAndLdapConfig(stack, ipaClient);
    }

    @Test
    public void testWiamCalledIfEntitlementEnabledAndNotInternal() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        Set<Node> nodes = Set.of(mock(Node.class));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        User user = new User();
        mockUsersyncCommonPart(stack, ipaClient, user);
        when(entitlementService.isWiamUsersyncRoutingEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.postInstallFreeIpa(1L, true);

        verifyNoInteractions(userSyncService);
        verifyUsersyncCommanPart(stack, gatewayConfig, nodes, ipaClient, user);
    }

    @Test
    public void testInternalUsersyncInvoked() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        Set<Node> nodes = Set.of(mock(Node.class));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        User user = new User();
        mockUsersyncCommonPart(stack, ipaClient, user);
        when(entitlementService.isWorkloadIamSyncEnabled(ACCOUNT_ID)).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.postInstallFreeIpa(1L, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(userSyncService).synchronizeUsers(eq(ACCOUNT_ID), anyString(), eq(Set.of(ENVIRONMENT_CRN)), eq(Set.of()), eq(Set.of()),
                eq(WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED));
        verifyUsersyncCommanPart(stack, gatewayConfig, nodes, ipaClient, user);
    }

    @Test
    public void testNoUsersyncInvoked() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        Set<Node> nodes = Set.of(mock(Node.class));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        User user = new User();
        mockUsersyncCommonPart(stack, ipaClient, user);
        when(entitlementService.isWorkloadIamSyncEnabled(ACCOUNT_ID)).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.postInstallFreeIpa(1L, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verifyNoInteractions(userSyncService);
        verifyUsersyncCommanPart(stack, gatewayConfig, nodes, ipaClient, user);
    }

    @Test
    public void testMaxHostnameLengthPolicyServiceInvoked() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        Set<Node> nodes = Set.of(mock(Node.class));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        User user = new User();
        mockUsersyncCommonPart(stack, ipaClient, user);
        Config ipaConfig = new Config();
        ipaConfig.setIpamaxusernamelength(5);
        ipaConfig.setIpamaxhostnamelength(64);
        when(ipaClient.getConfig()).thenReturn(ipaConfig);
        when(entitlementService.isWorkloadIamSyncEnabled(ACCOUNT_ID)).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.postInstallFreeIpa(1L, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verifyNoInteractions(userSyncService);
        verifyUsersyncCommanPart(stack, gatewayConfig, nodes, ipaClient, user);
        verify(hostnameLengthPolicyService).updateMaxHostnameLength(stack, ipaClient);
        verify(sidGenerationConfigurator).enableAndTriggerSidGeneration(stack, ipaClient);
    }

    private void mockUsersyncCommonPart(Stack stack, FreeIpaClient ipaClient, User user) throws FreeIpaClientException {
        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(freeIpaRecipeService.hasRecipeType(1L, RecipeType.POST_SERVICE_DEPLOYMENT, RecipeType.POST_CLUSTER_INSTALL)).thenReturn(false);
        when(freeIpaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack)).thenReturn(ipaClient);
        when(ipaClient.findPermission("Set Password Expiration")).thenReturn(Set.of());
        Config ipaConfig = new Config();
        ipaConfig.setIpamaxusernamelength(5);
        ipaConfig.setIpamaxhostnamelength(255);
        when(ipaClient.getConfig()).thenReturn(ipaConfig);
        when(freeIpaClientFactory.getAdminUser()).thenReturn("adminka");
        user.setUid("adminid");
        when(ipaClient.userFind("adminka")).thenReturn(Optional.of(user));
    }

    private void verifyUsersyncCommanPart(Stack stack, GatewayConfig gatewayConfig, Set<Node> nodes, FreeIpaClient ipaClient, User user)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException, FreeIpaClientException {
        verify(hostOrchestrator, never()).postServiceDeploymentRecipes(eq(gatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));
        verify(userSyncBindUserService).createUserAndLdapConfig(stack, ipaClient);
        verify(ipaClient).addPasswordExpirationPermission("Set Password Expiration");
        verify(ipaClient).addPermissionToPrivilege("User Administrators", "Set Password Expiration");
        verify(freeIpaPermissionService).setPermissions(stack, ipaClient);
        verify(ipaClient).setUsernameLength(255);
        verify(passwordPolicyService).updatePasswordPolicy(ipaClient);
        verify(ipaClient).updateUserPasswordMaxExpiration(user.getUid());
    }

    @Test
    public void postInstallFreeIpaExecutePostInstallRecipesButNoRecipes() throws Exception {
        Stack stack = mock(Stack.class);
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(freeIpaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack)).thenReturn(ipaClient);
        Set<Node> nodes = Set.of(mock(Node.class));
        when(freeIpaRecipeService.hasRecipeType(1L, RecipeType.POST_SERVICE_DEPLOYMENT, RecipeType.POST_CLUSTER_INSTALL)).thenReturn(false);
        when(userSyncBindUserService.doesBindUserAndConfigAlreadyExist(stack, ipaClient)).thenReturn(true);

        underTest.postInstallFreeIpa(1L, false);

        verify(hostOrchestrator, times(0)).postServiceDeploymentRecipes(eq(gatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));
        verify(userSyncBindUserService, never()).createUserAndLdapConfig(stack, ipaClient);
        verify(userSyncBindUserService).addBindUserToAdminGroup(stack, ipaClient);
        verifyNoInteractions(userSyncService);
    }

}