package com.sequenceiq.cloudbreak.service.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

class ClusterTemplateServiceCreationTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private ClusterTemplateRepository clusterTemplateRepository;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackTemplateService stackTemplateService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private Stack stack;

    @InjectMocks
    private ClusterTemplateService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    @Test
    void testWhenClusterTemplateDoesNotContainBlueprintBadRequestExceptionComes() {
        Workspace workspace = new Workspace();
        Blueprint blueprint = new Blueprint();
        blueprint.setName("test");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setStatus(ResourceStatus.USER_MANAGED);
        clusterTemplate.setWorkspace(workspace);
        when(clusterTemplateRepository.findByNameAndWorkspace(any(), any())).thenReturn(Optional.empty());
        when(blueprintService.getAllAvailableInWorkspace(workspace)).thenReturn(Set.of(blueprint));
        when(stackTemplateService.pureSave(stack)).thenReturn(stack);
        when(stack.getEnvironmentCrn()).thenReturn("someCrn");
        when(stack.getCluster()).thenReturn(cluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.prepareCreation(clusterTemplate));

        verify(stack).populateStackIdForComponents();
    }

}