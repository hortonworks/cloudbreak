package com.sequenceiq.cloudbreak.service.upgrade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
public class ImageComponentUpdaterServiceTest {

    private static final long STACK_ID = 1L;

    private static final String TARGET_IMAGE_ID = "targetImageId";

    private static final long WORKSPACE_ID = 8L;

    @Mock
    private StackService stackService;

    @Mock
    private StackComponentUpdater stackComponentUpdater;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private UpgradeImageInfoFactory upgradeImageInfoFactory;

    @Mock
    private ImageService imageService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private ComponentConverter componentConverter;

    @InjectMocks
    private ImageComponentUpdaterService underTest;

    @Mock
    private StatedImage targetStatedImage;

    @Mock
    private Image image;

    private final Stack stack = new Stack();

    private final Workspace workspace = new Workspace();

    @BeforeEach
    void setup() {
        setupStack();
        setupWorkspace();
        lenient().when(image.getUuid()).thenReturn(TARGET_IMAGE_ID);
        lenient().when(targetStatedImage.getImage()).thenReturn(image);
    }

    @Test
    void testUpgradeComponentsForUpdateWithImageId() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeImageInfo upgradeImageInfo = setupUpgradeImageInfo();
        when(upgradeImageInfoFactory.create(TARGET_IMAGE_ID, STACK_ID)).thenReturn(upgradeImageInfo);
        Set<Component> componentsToUpdate = getComponents();
        when(imageService.getComponentsWithoutUserData(stack, targetStatedImage)).thenReturn(componentsToUpdate);

        underTest.updateComponentsForUpgrade(TARGET_IMAGE_ID, STACK_ID);

        verify(restRequestThreadLocalService).setWorkspaceId(WORKSPACE_ID);
        verify(stackComponentUpdater).updateComponentsByStackId(stack, componentsToUpdate, true);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(stack, componentsToUpdate, true);
    }

    @Test
    void testUpgradeComponentsForUpdateWithStatedImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Set<Component> componentsToUpdate = getComponents();
        when(imageService.getComponentsWithoutUserData(stack, targetStatedImage)).thenReturn(componentsToUpdate);

        underTest.updateComponentsForUpgrade(targetStatedImage, STACK_ID);

        verify(restRequestThreadLocalService).setWorkspaceId(WORKSPACE_ID);
        verify(stackComponentUpdater).updateComponentsByStackId(stack, componentsToUpdate, true);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(stack, componentsToUpdate, true);
    }

    @ParameterizedTest
    @MethodSource(value = "possibleExceptions")
    void testUpgradeComponentsForUpdateWhenImageNotFoundThenNotFoundException(Class<? extends Exception> thrownException)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(upgradeImageInfoFactory.create(TARGET_IMAGE_ID, STACK_ID)).thenThrow(thrownException);

        Assertions.assertThatThrownBy(() -> underTest.updateComponentsForUpgrade(TARGET_IMAGE_ID, STACK_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Image 'targetImageId' not found.");

        verify(restRequestThreadLocalService).setWorkspaceId(WORKSPACE_ID);
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
    }

    @Test
    void testUpgradeComponentsForUpdateWhenStackNotFoundThenThrows() {
        when(stackService.getById(STACK_ID)).thenThrow(new NotFoundException("my message"));

        Assertions.assertThatThrownBy(() -> underTest.updateComponentsForUpgrade(TARGET_IMAGE_ID, STACK_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("my message");

        verify(restRequestThreadLocalService, never()).setWorkspaceId(any());
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
    }

    @ParameterizedTest
    @MethodSource(value = "possibleExceptions")
    void testUpgradeComponentsForUpdateWithImageIdWhenGetComponentsThrows(Class<? extends Exception> thrownException)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        UpgradeImageInfo upgradeImageInfo = setupUpgradeImageInfo();
        when(upgradeImageInfoFactory.create(TARGET_IMAGE_ID, STACK_ID)).thenReturn(upgradeImageInfo);
        when(imageService.getComponentsWithoutUserData(stack, targetStatedImage)).thenThrow(thrownException);

        Assertions.assertThatThrownBy(() -> underTest.updateComponentsForUpgrade(TARGET_IMAGE_ID, STACK_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Image 'targetImageId' not found.");

        verify(restRequestThreadLocalService).setWorkspaceId(WORKSPACE_ID);
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
    }

    @ParameterizedTest
    @MethodSource(value = "possibleExceptions")
    void testUpgradeComponentsForUpdateWhenGetComponentsThrows(Class<? extends Exception> thrownException)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageService.getComponentsWithoutUserData(stack, targetStatedImage)).thenThrow(thrownException);

        Assertions.assertThatThrownBy(() -> underTest.updateComponentsForUpgrade(targetStatedImage, STACK_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Image 'targetImageId' not found.");

        verify(restRequestThreadLocalService).setWorkspaceId(WORKSPACE_ID);
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
    }

    @Test
    void testUpdateComponentsUsesExplicitProductsAndCmRepository() {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName("CDH").withVersion("7.3.2-1.build")
                .withParcel("https://runtime-repo/");
        ClouderaManagerProduct cfm = new ClouderaManagerProduct().withName("CFM").withVersion("4.0.0")
                .withParcel("https://cfm-repo/");
        ClouderaManagerRepo cm = new ClouderaManagerRepo().withVersion("7.13.2").withBaseUrl("https://cm-repo/");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "base-image-version",
                OsType.RHEL9, "x86_64", cdh, Set.of(cfm), cm);
        Set<Component> parcelComponents = getComponents();
        Component cmComponent = new Component();
        cmComponent.setName("CM_REPO_DETAILS");
        when(componentConverter.fromClouderaManagerProductList(Set.of(cdh, cfm), stack)).thenReturn(parcelComponents);
        when(componentConverter.fromClouderaManagerRepo(cm, stack)).thenReturn(cmComponent);
        Set<Component> expected = new HashSet<>(parcelComponents);
        expected.add(cmComponent);

        underTest.updateComponentsForUpgrade(properties, STACK_ID);

        verify(restRequestThreadLocalService).setWorkspaceId(WORKSPACE_ID);
        verify(stackComponentUpdater).updateComponentsByStackId(stack, expected, true);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(stack, expected, true);
        verifyNoInteractions(imageService, upgradeImageInfoFactory);
    }

    private static Stream<Class<? extends Exception>> possibleExceptions() {
        return Stream.of(
                CloudbreakImageNotFoundException.class,
                CloudbreakImageCatalogException.class
        );
    }

    private Set<Component> getComponents() {
        Component component = new Component();
        component.setName("myComponent");
        return Set.of(component);
    }

    private UpgradeImageInfo setupUpgradeImageInfo() {
        UpgradeImageInfo upgradeImageInfo = mock(UpgradeImageInfo.class);
        when(upgradeImageInfo.targetStatedImage()).thenReturn(targetStatedImage);
        return upgradeImageInfo;
    }

    private void setupStack() {
        stack.setName("clusterName");
        stack.setWorkspace(workspace);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
    }

    private void setupWorkspace() {
        workspace.setId(WORKSPACE_ID);
    }

}
