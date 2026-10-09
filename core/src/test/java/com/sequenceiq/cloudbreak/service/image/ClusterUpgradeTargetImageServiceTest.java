package com.sequenceiq.cloudbreak.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeTargetImageServiceTest {

    private static final long STACK_ID = 1L;

    private static final String TARGET_IMAGE = "TARGET_IMAGE";

    @InjectMocks
    private ClusterUpgradeTargetImageService underTest;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ImageConverter imageConverter;

    @Mock
    private UserDataService userDataService;

    @Mock
    private Stack stack;

    @Test
    void testSaveImageFromPropertiesWithoutCatalogRepositoryDetails() throws IOException {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        when(stackDtoService.getStackReferenceById(STACK_ID)).thenReturn(stack);

        underTest.saveImage(STACK_ID, properties);

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProviderService).store(captor.capture());
        assertThat(captor.getValue().getComponentType()).isEqualTo(ComponentType.IMAGE);
        assertThat(captor.getValue().getName()).isEqualTo(TARGET_IMAGE);
        assertThat(captor.getValue().getAttributes().get(Image.class)).isEqualTo(properties.toCloudImage());
        verify(userDataService).makeSureUserDataIsMigrated(STACK_ID);
    }

    @Test
    void testSaveImageReplacesDifferentTarget() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        Image oldImage = Image.builder().withImageId("old-image").build();
        Component existing = new Component(ComponentType.IMAGE, TARGET_IMAGE, new Json(oldImage), stack);
        when(stackImageService.findImageComponentByName(STACK_ID, TARGET_IMAGE)).thenReturn(Optional.of(existing));
        when(imageConverter.convertJsonToImage(existing.getAttributes())).thenReturn(oldImage);
        when(stackDtoService.getStackReferenceById(STACK_ID)).thenReturn(stack);

        underTest.saveImage(STACK_ID, properties);

        verify(componentConfigProviderService).deleteComponents(Set.of(existing));
        verify(componentConfigProviderService).store(any(Component.class));
    }

    @Test
    void testSaveImageReplacesSameImageWithDifferentPackageVersions() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        Image oldImage = properties.toCloudImage();
        oldImage.getPackageVersions().put("stack", "7.2.18");
        Component existing = new Component(ComponentType.IMAGE, TARGET_IMAGE, new Json(oldImage), stack);
        when(stackImageService.findImageComponentByName(STACK_ID, TARGET_IMAGE)).thenReturn(Optional.of(existing));
        when(imageConverter.convertJsonToImage(existing.getAttributes())).thenReturn(oldImage);
        when(stackDtoService.getStackReferenceById(STACK_ID)).thenReturn(stack);

        underTest.saveImage(STACK_ID, properties);

        verify(componentConfigProviderService).deleteComponents(Set.of(existing));
        verify(componentConfigProviderService).store(any(Component.class));
    }

    @Test
    void testSaveImageSkipsIdenticalTarget() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        Image targetImage = properties.toCloudImage();
        Component existing = new Component(ComponentType.IMAGE, TARGET_IMAGE, new Json(targetImage), stack);
        when(stackImageService.findImageComponentByName(STACK_ID, TARGET_IMAGE)).thenReturn(Optional.of(existing));
        when(imageConverter.convertJsonToImage(existing.getAttributes())).thenReturn(targetImage);

        underTest.saveImage(STACK_ID, properties);

        verifyNoInteractions(stackDtoService, componentConfigProviderService, userDataService);
    }
}
