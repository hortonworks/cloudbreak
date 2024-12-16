package com.sequenceiq.cloudbreak.service.template;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ClusterTemplateViewToClusterTemplateViewV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.init.clustertemplate.ClusterTemplateLoaderService;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

@ExtendWith(MockitoExtension.class)
public class ClusterTemplateServiceDeletionTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String TEMPLATE_NAME = "name";

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private ClusterTemplateRepository clusterTemplateRepository;

    @Mock
    private ClusterTemplateViewService clusterTemplateViewService;

    @Mock
    private UserService userService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private ClusterTemplateLoaderService clusterTemplateLoaderService;

    @Mock
    private OrchestratorService orchestratorService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private NetworkService networkService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackTemplateService stackTemplateService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private SupportedRuntimes supportedRuntimes;

    @Mock
    private ClusterTemplateCloudPlatformValidator cloudPlatformValidator;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private ClusterTemplateViewToClusterTemplateViewV4ResponseConverter clusterTemplateViewToClusterTemplateViewV4ResponseConverter;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private InternalClusterTemplateValidator internalClusterTemplateValidator;

    @Mock
    private DatabaseService databaseService;

    @InjectMocks
    private ClusterTemplateService underTest;

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ResourceStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"DEFAULT_DELETED", "DEFAULT"})
    void prepareDeletionTestWhenSuccessAndNotInternal(ResourceStatus resourceStatus) {
        ClusterTemplate resource = createClusterTemplate(resourceStatus, FeatureState.RELEASED);

        when(entitlementService.internalTenant(ACCOUNT_ID)).thenReturn(false);
        when(internalClusterTemplateValidator.isInternalTemplateInNotInternalTenant(false, FeatureState.RELEASED)).thenReturn(false);

        underTest.prepareDeletion(resource);
    }

    private static ClusterTemplate createClusterTemplate(ResourceStatus resourceStatus, FeatureState featureState) {
        ClusterTemplate resource = new ClusterTemplate();
        resource.setStatus(resourceStatus);
        Workspace workspace = new Workspace();
        resource.setWorkspace(workspace);
        Tenant tenant = new Tenant();
        workspace.setTenant(tenant);
        tenant.setName(ACCOUNT_ID);
        resource.setFeatureState(featureState);
        return resource;
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ResourceStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"DEFAULT_DELETED", "DEFAULT"})
    void prepareDeletionTestWhenSuccessAndInternal(ResourceStatus resourceStatus) {
        ClusterTemplate resource = createClusterTemplate(resourceStatus, FeatureState.INTERNAL);
        resource.setName(TEMPLATE_NAME);

        when(entitlementService.internalTenant(ACCOUNT_ID)).thenReturn(true);
        when(internalClusterTemplateValidator.isInternalTemplateInNotInternalTenant(true, FeatureState.INTERNAL)).thenReturn(true);

        underTest.prepareDeletion(resource);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ResourceStatus.class, names = {"DEFAULT_DELETED", "DEFAULT"})
    void prepareDeletionTestWhenErrorAndDefault(ResourceStatus resourceStatus) {
        ClusterTemplate resource = new ClusterTemplate();
        resource.setStatus(resourceStatus);
        resource.setName(TEMPLATE_NAME);

        AccessDeniedException accessDeniedException = assertThrows(AccessDeniedException.class, () -> underTest.prepareDeletion(resource));

        assertThat(accessDeniedException.getMessage()).isEqualTo(format("Default cluster definition deletion is forbidden: '%s'", TEMPLATE_NAME));
    }

}
