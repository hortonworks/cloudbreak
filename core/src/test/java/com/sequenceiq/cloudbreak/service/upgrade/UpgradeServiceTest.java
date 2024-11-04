package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class UpgradeServiceTest {

    private static final String CLUSTER_CRN = "cluster-crn";

    private static final long WORKSPACE_ID = 0L;

    private static final NameOrCrn OF_CRN = NameOrCrn.ofName(CLUSTER_CRN);

    private static final String IMAGE_ID = "imageId";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterRepairService clusterRepairService;

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private ComponentVersionProvider componentVersionProvider;

    @Mock
    private LockedComponentService lockedComponentService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private UpgradeService underTest;

    @Mock
    private User user;

    @Mock
    private EntitlementService entitlementService;

    @Captor
    private ArgumentCaptor<ImageSettingsV4Request> captor;

    @Test
    public void testPrepareUpgradeLockedComponents() {
        StackDto stack = getStackDto();
        when(stackDtoService.getByNameOrCrn(OF_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, IMAGE_ID)).thenReturn(Boolean.TRUE);

        assertThrows(BadRequestException.class, () -> underTest.prepareClusterUpgrade(ACCOUNT_ID, OF_CRN, IMAGE_ID));
        verifyNoInteractions(flowManager);
    }

    @Test
    public void testPrepareUpgradeImageNotFound() throws CloudbreakImageNotFoundException {
        StackDto stack = getStackDto();
        when(stackDtoService.getByNameOrCrn(OF_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, IMAGE_ID)).thenReturn(Boolean.FALSE);
        when(componentConfigProviderService.getImage(stack.getId())).thenThrow(new CloudbreakImageNotFoundException("nope"));

        assertThrows(NotFoundException.class, () -> underTest.prepareClusterUpgrade(ACCOUNT_ID, OF_CRN, IMAGE_ID));
        verifyNoInteractions(flowManager);
    }

    @Test
    public void testPrepareUpgradeTriggersFlow() throws CloudbreakImageNotFoundException {
        StackDto stack = getStackDto();
        when(stackDtoService.getByNameOrCrn(OF_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, IMAGE_ID)).thenReturn(Boolean.FALSE);
        Image image = mock(Image.class);
        String imgCatName = "imgCatName";
        when(image.getImageCatalogName()).thenReturn(imgCatName);
        String imgCatUrl = "imgCatUrl";
        when(image.getImageCatalogUrl()).thenReturn(imgCatUrl);
        when(image.getPackageVersion(ImagePackageVersion.STACK)).thenReturn("runtime");
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(image);
        ArgumentCaptor<ImageChangeDto> imageChangeDtoArgumentCaptor = ArgumentCaptor.forClass(ImageChangeDto.class);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollId");
        when(flowManager.triggerClusterUpgradePreparation(eq(stack.getId()), imageChangeDtoArgumentCaptor.capture(), eq("runtime")))
                .thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.prepareClusterUpgrade(ACCOUNT_ID, OF_CRN, IMAGE_ID);

        assertEquals(flowIdentifier, result);
        ImageChangeDto imageChangeDto = imageChangeDtoArgumentCaptor.getValue();
        assertEquals(imgCatName, imageChangeDto.getImageCatalogName());
        assertEquals(imgCatUrl, imageChangeDto.getImageCatalogUrl());
        assertEquals(IMAGE_ID, imageChangeDto.getImageId());
        assertEquals(stack.getId(), imageChangeDto.getStackId());
    }

    private StackDto getStackDto() {
        StackDto stackDto = spy(StackDto.class);
        Stack stack = getStack();
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getWorkspace()).thenReturn(stack.getWorkspace());
        return stackDto;
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setEnvironmentCrn("env-crn");
        stack.setCloudPlatform("AWS");
        stack.setPlatformVariant("AWS");
        stack.setRegion("eu-central-1");
        Blueprint blueprint = new Blueprint();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        stack.setArchitecture(Architecture.ARM64);
        return stack;
    }

}