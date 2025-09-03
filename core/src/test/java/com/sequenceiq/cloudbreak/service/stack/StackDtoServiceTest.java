package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.ClusterDtoRepository;
import com.sequenceiq.cloudbreak.repository.StackDtoRepository;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.view.AvailabilityZoneView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.delegate.ClusterViewDelegate;
import com.sequenceiq.cloudbreak.view.delegate.StackViewDelegate;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StackDtoServiceTest {

    private static final Long ID1 = 1L;

    private static final Long ID2 = 2L;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackDtoRepository stackDtoRepository;

    @Mock
    private ClusterDtoRepository clusterDtoRepository;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private GatewayService gatewayService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private OrchestratorService orchestratorService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private StackParametersService stackParametersService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @InjectMocks
    private StackDtoService underTest;

    @Mock
    private StackViewDelegate stackViewDelegate;

    @Mock
    private ClusterViewDelegate clusterViewDelegate;

    @Mock
    private InstanceGroupView group1;

    @Mock
    private InstanceGroupView group2;

    @Mock
    private InstanceMetadataView instance1;

    @Mock
    private InstanceMetadataView instance2;

    @Mock
    private AvailabilityZoneView az1;

    @Mock
    private AvailabilityZoneView az2;

    @BeforeEach
    void setUp() {
        when(stackViewDelegate.getId()).thenReturn(ID1);
        when(clusterViewDelegate.getId()).thenReturn(ID1);

        when(group1.getId()).thenReturn(ID1);
        when(group1.getGroupName()).thenReturn("group1");
        when(instance1.getId()).thenReturn(ID1);
        when(instance1.getInstanceGroupId()).thenReturn(ID1);
        when(az1.getAvailabilityZone()).thenReturn("az1");

        when(group2.getId()).thenReturn(ID2);
        when(group2.getGroupName()).thenReturn("group2");
        when(instance2.getId()).thenReturn(ID2);
        when(instance2.getInstanceGroupId()).thenReturn(ID2);
        when(az2.getAvailabilityZone()).thenReturn("az2");
    }

    @Test
    void testGetByName() {
        when(stackDtoRepository.findByName("account1", "stack1")).thenReturn(Optional.of(stackViewDelegate));
        when(clusterDtoRepository.findByStackId(anyLong())).thenReturn(Optional.of(clusterViewDelegate));
        when(workspaceService.getByIdWithoutAuth(any())).thenReturn(new Workspace());
        when(instanceGroupService.getInstanceGroupViewByStackId(anyLong())).thenReturn(List.of(group1, group2));
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(instance1, instance2));
        when(instanceGroupService.getAvailabilityZonesByStackId(anyLong())).thenReturn(Map.of(
                ID1, List.of(az1, az2),
                ID2, List.of(az1)
        ));

        StackDto stackDto = underTest.getByNameOrCrn(NameOrCrn.ofName("stack1"), "account1");

        assertNotNull(stackDto);
        assertEquals(group1, stackDto.getInstanceGroupByInstanceGroupName("group1").getInstanceGroup());
        assertEquals(List.of(instance1), stackDto.getInstanceGroupByInstanceGroupName("group1").getInstanceMetadataViews());
        assertEquals(group2, stackDto.getInstanceGroupByInstanceGroupName("group2").getInstanceGroup());
        assertEquals(List.of(instance2), stackDto.getInstanceGroupByInstanceGroupName("group2").getInstanceMetadataViews());
        assertEquals(List.of("az1", "az2"), stackDto.getAvailabilityZonesByInstanceGroup().get(group1));
        assertEquals(List.of("az1"), stackDto.getAvailabilityZonesByInstanceGroup().get(group2));
    }

    @Test
    void testGetInstanceMetadataByInstanceGroup() {
        when(instanceGroupService.getInstanceGroupViewByStackId(anyLong())).thenReturn(List.of(group1, group2));
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(instance1, instance2));

        List<InstanceGroupDto> result = underTest.getInstanceMetadataByInstanceGroup(ID1);

        assertEquals(2, result.size());
        assertEquals(group1, result.get(0).getInstanceGroup());
        assertEquals(List.of(instance1), result.get(0).getInstanceMetadataViews());
        assertEquals(group2, result.get(1).getInstanceGroup());
        assertEquals(List.of(instance2), result.get(1).getInstanceMetadataViews());
    }

    @Test
    void testGetSdxBasicView() {
        when(stackDtoRepository.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(stackViewDelegate));
        when(clusterDtoRepository.findByStackId(anyLong())).thenReturn(Optional.of(clusterViewDelegate));
        when(runtimeVersionService.getRuntimeVersion(anyLong())).thenReturn(Optional.of("7.2.18"));
        when(workspaceService.getByIdWithoutAuth(any())).thenReturn(new Workspace());

        Optional<SdxBasicView> sdx = underTest.getSdxBasicView("env");

        assertTrue(sdx.isPresent());
        assertEquals(sdx.get().runtime(), "7.2.18");
    }

    @Test
    void testGetSdxBasicViewReturnsInTheCorrectOrder() {
        StackViewDelegate stack1 = mock(StackViewDelegate.class);
        StackViewDelegate stack2 = mock(StackViewDelegate.class);
        StackViewDelegate stack3 = mock(StackViewDelegate.class);

        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stack2.getOriginalName()).thenReturn("stack2");
        when(stack3.getName()).thenReturn("stack3");

        when(stackDtoRepository.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(stack1, stack2, stack3));
        when(clusterDtoRepository.findByStackId(anyLong())).thenReturn(Optional.of(clusterViewDelegate));
        when(workspaceService.getByIdWithoutAuth(any())).thenReturn(new Workspace());

        Optional<SdxBasicView> sdx = underTest.getSdxBasicView("env");

        assertTrue(sdx.isPresent());
        assertEquals("stack2", sdx.get().name());
    }

    @Test
    void testFindAllByResourceCrnInWithoutResources() {
        when(stackViewDelegate.getResourceCrn()).thenReturn("crn1");
        when(stackDtoRepository.findAllByResourceCrnIn(List.of("crn1"))).thenReturn(List.of(stackViewDelegate));
        when(clusterDtoRepository.findByStackId(anyLong())).thenReturn(Optional.of(clusterViewDelegate));
        when(workspaceService.getByIdWithoutAuth(any())).thenReturn(new Workspace());

        List<StackDto> result = underTest.findAllByResourceCrnInWithoutResources(List.of("crn1"));

        verifyNoInteractions(resourceService);
        assertEquals("crn1", result.getFirst().getResourceCrn());
    }
}