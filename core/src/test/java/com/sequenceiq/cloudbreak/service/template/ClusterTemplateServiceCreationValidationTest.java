package com.sequenceiq.cloudbreak.service.template;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

class ClusterTemplateServiceCreationValidationTest {

    private static final long WORKSPACE_ID = 1L;

    private static final String ACCOUNT_ID = "someAccountId";

    private static final String CREATOR_ID = "someUserId";

    @Mock
    private ClusterTemplateRepository clusterTemplateRepository;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private InternalClusterTemplateValidator internalClusterTemplateValidator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @InjectMocks
    private ClusterTemplateService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(any(), any());
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
        try {
            when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
                Supplier<ClusterTemplate> arg = (Supplier<ClusterTemplate>) invocation.getArguments()[0];
                return arg.get();
            });
        } catch (TransactionService.TransactionExecutionException e) {
            //could not happen, mocked
        }
    }

    @Test
    void testWhenClusterTemplateDoesNotContainAClusterBadRequestExceptionComes() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("someCrn");
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        when(clusterTemplateRepository.findByNameAndWorkspace(any(), any())).thenReturn(Optional.empty());
        when(entitlementService.internalTenant(anyString())).thenReturn(true);
        when(internalClusterTemplateValidator
                .isInternalTemplateInNotInternalTenant(anyBoolean(), any(FeatureState.class))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(clusterTemplate,
                WORKSPACE_ID, ACCOUNT_ID, CREATOR_ID),
                "Datahub template in cluster definition should contain a – valid – cluster request!");
    }

    @Test
    void testWhenClusterTemplateDoesNotContainAnExistingBlueprintBadRequestExceptionComes() {
        Cluster cluster = new Cluster();
        Blueprint blueprint = new Blueprint();
        blueprint.setName("apple");
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setEnvironmentCrn("someCrn");
        stack.setCluster(cluster);
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        when(clusterTemplateRepository.findByNameAndWorkspace(any(), any())).thenReturn(Optional.empty());
        when(entitlementService.internalTenant(anyString())).thenReturn(true);
        when(internalClusterTemplateValidator
                .isInternalTemplateInNotInternalTenant(anyBoolean(), any(FeatureState.class))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(clusterTemplate,
                WORKSPACE_ID, ACCOUNT_ID, CREATOR_ID),
                "The Datahub template in the cluster definition must exist!");
    }

    @Test
    void testWhenClusterTemplateDoesNotContainBlueprintBadRequestExceptionComes() {
        Workspace workspace = new Workspace();
        Blueprint blueprint = new Blueprint();
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setEnvironmentCrn("someCrn");
        stack.setCluster(cluster);
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        clusterTemplate.setWorkspace(workspace);
        when(clusterTemplateRepository.findByNameAndWorkspace(any(), any())).thenReturn(Optional.empty());
        when(blueprintService.getAllAvailableInWorkspace(workspace)).thenReturn(Collections.emptySet());
        when(entitlementService.internalTenant(anyString())).thenReturn(true);
        when(internalClusterTemplateValidator
                .isInternalTemplateInNotInternalTenant(anyBoolean(), any(FeatureState.class))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(clusterTemplate,
                WORKSPACE_ID, ACCOUNT_ID, CREATOR_ID),
                "Cluster definition should contain a Datahub template!");
    }

}