package com.sequenceiq.cloudbreak.security;

import static java.util.Collections.EMPTY_LIST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;
import com.sequenceiq.cloudbreak.repository.security.UserRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.security.OwnerBasedPermissionEvaluator;
import com.sequenceiq.cloudbreak.service.stack.StackDownscaleValidatorService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@SpringBootTest(classes = StackServiceSecurityComponentTest.TestConfig.class)
public class StackServiceSecurityComponentTest extends SecurityComponentTestBase {

    public static final long ANY_ID = 1L;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private StackViewRepository stackViewRepository;

    @Inject
    private StackStatusRepository stackStatusRepository;

    @Inject
    private HasPermissionAspectForMockitoTest hasPermissionAspectForMockitoTest;

    @Inject
    private StackService underTest;

    @SpyBean
    private OwnerBasedPermissionEvaluator ownerBasedPermissionEvaluator;

    @Inject
    private ReactorFlowManager flowManager;

    @Test(expected = AccessDeniedException.class)
    public void testRetrievePrivateStacksNotAccessibleForUserOfDifferentAccount() throws Exception {
        Set<Stack> foundStacks = new HashSet(Collections.singleton(getAStack()));
        when(stackRepository.findForUserWithLists(anyString())).thenReturn(foundStacks);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrievePrivateStacks(loggedInUser);

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test
    public void testRetrievePrivateStacks() throws Exception {
        Set<Stack> foundStacks = new HashSet(Collections.singleton(getAStack()));
        when(stackRepository.findForUserWithLists(anyString())).thenReturn(foundStacks);
        IdentityUser loggedInUser = getOwner(false);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrievePrivateStacks(loggedInUser);

        }

        verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
    }

    @Test(expected = AccessDeniedException.class)
    public void testRetrieveAccountStacksNotAccessibleForUserOfDifferentAccount() throws Exception {
        Set<Stack> foundStacks = new HashSet(Collections.singleton(getAStack()));
        when(stackRepository.findAllInAccountWithLists(anyString())).thenReturn(foundStacks);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrieveAccountStacks(loggedInUser);

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testRetrieveAccountStacksNotAccessibleForUserOfDifferentAccountAsNotAdmin() throws Exception {
        Set<Stack> foundStacks = new HashSet(Collections.singleton(getAStack()));
        when(stackRepository.findPublicInAccountForUser(anyString(), anyString())).thenReturn(foundStacks);
        IdentityUser loggedInUser = getUserFromDifferentAccount();
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrieveAccountStacks(loggedInUser);

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testRetrieveAccountStacksForAccountNotAccessibleForUserOfDifferentAccount() throws Exception {
        Set<Stack> foundStacks = new HashSet(Collections.singleton(getAStack()));
        when(stackRepository.findAllInAccount(anyString())).thenReturn(foundStacks);
        setupLoggedInUser(getUserFromDifferentAccount());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrieveAccountStacks("account");

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testRetrieveOwnerStacksNotAccessibleForUserOfDifferentAccount() throws Exception {
        Set<Stack> foundStacks = new HashSet(Collections.singleton(getAStack()));
        when(stackRepository.findForUser(anyString())).thenReturn(foundStacks);
        setupLoggedInUser(getUserFromDifferentAccount());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.retrieveOwnerStacks("owner");

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdWithListsNotAccessibleForUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findOneWithLists(ANY_ID)).thenReturn(foundStack);
        setupLoggedInUser(getUserFromDifferentAccount());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getByIdWithLists(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetJsonByIdNotAccessibleForUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findOneWithLists(ANY_ID)).thenReturn(foundStack);
        setupLoggedInUser(getUserFromDifferentAccount());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getJsonById(ANY_ID, Collections.emptyList());

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetNotAccessibleForUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findById(ANY_ID)).thenReturn(Optional.of(foundStack));
        setupLoggedInUser(getUserFromDifferentAccount());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.get(ANY_ID);

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(Optional.of(foundStack)), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetForAutoscaleNotAccessibleForUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findById(ANY_ID)).thenReturn(Optional.of(foundStack));
        setupLoggedInUser(getUserFromDifferentAccount(true, "cloudbreak.autoscale"));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getForAutoscale(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(Optional.of(foundStack)), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetAllForAutoscaleNotAccessibleForUserOfDifferentAccount() throws Exception {
        Set<Stack> foundStacks = new HashSet<>(Collections.singletonList(getAStack()));
        when(stackRepository.findAliveOnes()).thenReturn(foundStacks);
        setupLoggedInUser(getUserFromDifferentAccount(true, "cloudbreak.autoscale"));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getAllForAutoscale();

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test
    public void testGetAllForAutoscale() throws Exception {
        Set<Stack> foundStacks = new HashSet<>(Collections.singletonList(getAStack()));
        when(stackRepository.findAliveOnes()).thenReturn(foundStacks);
        setupLoggedInUser(getOwner(false, "cloudbreak.autoscale"));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getAllForAutoscale();

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testFindClustersConnectedToDatalakeNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Set<Stack> foundStacks = new HashSet<>(Collections.singletonList(getAStack()));
        when(stackRepository.findEphemeralClusters(ANY_ID)).thenReturn(foundStacks);
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.findClustersConnectedToDatalake(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStacks), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdWithListsNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findOneWithLists(ANY_ID)).thenReturn(foundStack);
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getByIdWithLists(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdWithListsThrowsWhenNotFound() throws Exception {
        when(stackRepository.findOneWithLists(ANY_ID)).thenReturn(null);
        setupLoggedInUser(getAUser());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getByIdWithLists(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(null), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Optional<Stack> foundStack = Optional.of(getAStack());
        when(stackRepository.findById(ANY_ID)).thenReturn(foundStack);
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getById(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdViewNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Optional<StackView> foundStackView = Optional.of(getAStackView());
        when(stackViewRepository.findById(ANY_ID)).thenReturn(foundStackView);
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getByIdView(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStackView), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdViewThrowsWhenNotFound() throws Exception {
        when(stackViewRepository.findById(ANY_ID)).thenReturn(Optional.empty());
        setupLoggedInUser(getAUser());

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getByIdView(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(Optional.empty()), eq(PERMISSION_READ));
        }
    }

    @Test
    public void testGetCurrentStatusByStackId() throws Exception {
        StackStatus stackStatus = getAStackStatus();
        when(stackStatusRepository.findFirstByStackIdOrderByCreatedDesc(ANY_ID)).thenReturn(stackStatus);
        setupLoggedInUser(getUserFromDifferentAccount(false));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getCurrentStatusByStackId(ANY_ID);

        } finally {
            verify(ownerBasedPermissionEvaluator, never()).hasPermission(any(), eq(stackStatus), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetByIdAmbariAddressNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByAmbari("ambariAddress")).thenReturn(foundStack);
        setupLoggedInUser(getUserFromDifferentAccount(true));

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getByAmbariAddress("ambariAddress");

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPrivarteStackNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByNameInUser(anyString(), anyString())).thenReturn(foundStack);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPrivateStack("name", loggedInUser);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPrivateStackJsonByNameNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByNameInUserWithLists(anyString(), anyString())).thenReturn(foundStack);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPrivateStackJsonByName("name", loggedInUser, EMPTY_LIST);

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPublicStackJsonByNameNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByNameInAccountWithLists(anyString(), anyString())).thenReturn(foundStack);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPublicStackJsonByName("name", loggedInUser, EMPTY_LIST);

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetStackRequestByNameNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByNameInAccountWithLists(anyString(), anyString())).thenReturn(foundStack);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getStackRequestByName("name", loggedInUser);

        } catch (TransactionService.TransactionRuntimeExecutionException e) {
            throw getRootCauseOfTransactionException(e);
        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetPublicStackNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByNameInAccount(anyString(), anyString())).thenReturn(foundStack);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.getPublicStack("name", loggedInUser);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteByNameNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByNameInAccountOrOwner(anyString(), anyString(), anyString())).thenReturn(foundStack);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.delete("name", loggedInUser, true, true);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
            verify(flowManager, never()).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testDeleteByIdNotAccessibleForAdminUserOfDifferentAccount() throws Exception {
        Stack foundStack = getAStack();
        when(stackRepository.findByIdInAccount(anyLong(), anyString())).thenReturn(foundStack);
        IdentityUser loggedInUser = getUserFromDifferentAccount(true);
        setupLoggedInUser(loggedInUser);

        try (HasPermissionAspectForMockitoTest.StubbingDeactivator deactivator = hasPermissionAspectForMockitoTest.new StubbingDeactivator()) {
            underTest.delete(ANY_ID, loggedInUser, true, true);

        } finally {
            verify(ownerBasedPermissionEvaluator).hasPermission(any(), eq(foundStack), eq(PERMISSION_READ));
            verify(flowManager, never()).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        }
    }

    private Exception getRootCauseOfTransactionException(TransactionService.TransactionRuntimeExecutionException e) {
        return (Exception) e.getCause().getCause();
    }

    private Stack getAStack() {
        Stack stack = new Stack();
        stack.setOwner(USER_A_ID);
        stack.setAccount(ACCOUNT_A);
        stack.setPublicInAccount(false);
        return stack;
    }

    private StackView getAStackView() {
        return new StackView(1L, "", USER_A_ID, "", null);
    }

    private StackStatus getAStackStatus() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStack(getAStack());
        return stackStatus;
    }

    private StackResponse getAStackResponse() {
        StackResponse stackResponse = new StackResponse();
        stackResponse.setOwner(USER_A_ID);
        stackResponse.setAccount(ACCOUNT_A);
        stackResponse.setPublicInAccount(false);
        return stackResponse;
    }

    @Configuration
    @ComponentScan(basePackages =
            {"com.sequenceiq.cloudbreak"},
            useDefaultFilters = false,
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                            StackService.class,
                            TransactionService.class,
                            Clock.class,
                            StackDownscaleValidatorService.class,
                    })
            })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    public static class TestConfig extends SecurityComponentTestBase.SecurityComponentTestBaseConfig {

        @MockBean
        private StackUpdater stackUpdater;

        @MockBean
        private ImageService imageService;

        @MockBean
        private ClusterService ambariClusterService;

        @MockBean
        private ClusterRepository clusterRepository;

        @MockBean
        private InstanceMetaDataRepository instanceMetaDataRepository;

        @MockBean
        private InstanceGroupRepository instanceGroupRepository;

        @MockBean
        private OrchestratorRepository orchestratorRepository;

        @MockBean
        private TlsSecurityService tlsSecurityService;

        @MockBean
        private ReactorFlowManager flowManager;

        @MockBean
        private BlueprintValidator blueprintValidator;

        @MockBean
        private NetworkConfigurationValidator networkConfigurationValidator;

        @MockBean
        private CloudbreakEventService eventService;

        @MockBean
        private CloudbreakMessagesService cloudbreakMessagesService;

        @MockBean
        private ServiceProviderConnectorAdapter connector;

        @MockBean
        private ContainerOrchestratorResolver containerOrchestratorResolver;

        @MockBean
        private ComponentConfigProvider componentConfigProvider;

        @MockBean
        private SecurityConfigRepository securityConfigRepository;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private StackResponseDecorator stackResponseDecorator;

        @MockBean
        private OpenSshPublicKeyValidator rsaPublicKeyValidator;

        @MockBean(name = "conversionService")
        private ConversionService conversionService;

        @Bean
        public StackRepository stackRepository() {
            return mock(StackRepository.class);
        }

        @Bean
        public StackViewRepository stackViewRepository() {
            return mock(StackViewRepository.class);
        }

        @Bean
        public StackStatusRepository stackStatusRepository() {
            return mock(StackStatusRepository.class);
        }
    }
}