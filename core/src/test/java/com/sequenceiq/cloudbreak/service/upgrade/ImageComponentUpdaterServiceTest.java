package com.sequenceiq.cloudbreak.service.upgrade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

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

    @InjectMocks
    private ImageComponentUpdaterService underTest;

    @Mock
    private StatedImage targetStatedImage;

    private final Stack stack = new Stack();

    private final Workspace workspace = new Workspace();

    @BeforeEach
    void setup() {
        setupStack();
        setupWorkspace();
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
        Image image = mock(Image.class);
        when(targetStatedImage.getImage()).thenReturn(image);

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
        Image image = mock(Image.class);
        when(image.getUuid()).thenReturn(TARGET_IMAGE_ID);
        when(targetStatedImage.getImage()).thenReturn(image);

        Assertions.assertThatThrownBy(() -> underTest.updateComponentsForUpgrade(targetStatedImage, STACK_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Image 'targetImageId' not found.");

        verify(restRequestThreadLocalService).setWorkspaceId(WORKSPACE_ID);
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
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
