package com.sequenceiq.cloudbreak.security;

import static org.mockito.Mockito.mock;

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

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
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
import com.sequenceiq.cloudbreak.repository.workspace.UserRepository;
import com.sequenceiq.cloudbreak.security.StackServiceSecurityComponentTest.TestConfig;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.security.TenantBasedPermissionEvaluator;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.stack.StackDownscaleValidatorService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.user.UserService;

@SpringBootTest(classes = TestConfig.class)
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
    private TenantBasedPermissionEvaluator tenantBasedPermissionEvaluator;

    @MockBean
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private ReactorFlowManager flowManager;

    @Test
    public void dummy() {

    }

    private Exception getRootCauseOfTransactionException(TransactionRuntimeExecutionException e) {
        return (Exception) e.getCause().getCause();
    }

    private Stack getAStack() {
        Stack stack = new Stack();
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        return stack;
    }

    private StackView getAStackView() {
        return new StackView(1L, "", "", null);
    }

    private StackStatus getAStackStatus() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStack(getAStack());
        return stackStatus;
    }

    private StackResponse getAStackResponse() {
        return new StackResponse();
    }

    private Workspace defaultWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setName("Hello");
        workspace.setId(1L);
        return workspace;
    }

    @Configuration
    @ComponentScan(basePackages = "com.sequenceiq.cloudbreak", useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                    StackService.class,
                    TransactionService.class,
                    Clock.class,
                    StackDownscaleValidatorService.class,
            }))
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    public static class TestConfig extends SecurityComponentTestBaseConfig {

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

        @MockBean
        private UserService userService;

        @MockBean
        private WorkspaceService workspaceService;

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