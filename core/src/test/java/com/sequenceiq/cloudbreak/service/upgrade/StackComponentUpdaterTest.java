package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.ImageTestUtil;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class StackComponentUpdaterTest {

    private static final String TARGET_STACK_VERSION = "7.2.0";

    private static final String STACK_VERSION = "7.1.0";

    @Mock
    private ImageService imageService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @InjectMocks
    private StackComponentUpdater underTest;

    @Test
    public void testUpdateImageComponents() {

        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        StatedImage targetImage = ImageTestUtil.getImageFromCatalog(true, "targetImageUuid", TARGET_STACK_VERSION);

        Image originalImage = ImageTestUtil.getImage(true, "originalImageUuid", STACK_VERSION, null);
        Component originalImageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(originalImage), stack);

        when(componentConfigProviderService.getComponentsByStackId(stack.getId())).thenReturn(Set.of(originalImageComponent));
        Set<Component> targetComponents = createComponents(stack, targetImage);

        underTest.updateComponentsByStackId(stack, targetComponents, true);

        ArgumentCaptor<Set<Component>> componentCatcher = ArgumentCaptor.forClass(Set.class);
        verify(componentConfigProviderService, times(1)).store(componentCatcher.capture());
        assertEquals(3, componentCatcher.getValue().size());
        assertTrue(componentCatcher.getValue().stream().anyMatch(
                component -> {
                    Object version = component.getAttributes().getValue("version");
                    if (Objects.nonNull(version)) {
                        return ((String) version).contains(TARGET_STACK_VERSION);
                    } else {
                        return false;
                    }
                }));
        assertTrue(componentCatcher.getValue().stream().anyMatch(
                component -> {
                    Object userData = component.getAttributes().getValue("userdata");
                    if (Objects.nonNull(userData)) {
                        return ((Map<String, String>) userData).get(InstanceGroupType.GATEWAY.name()).contains("gw user data");
                    } else {
                        return false;
                    }
                }));

    }

    private Set<Component> createComponents(Stack stack, StatedImage statedImage) {
        return Set.of(createImageComponent(statedImage, stack), createStackRepoComponent(stack), createCMComponennt(stack));
    }

    private Component createImageComponent(StatedImage statedImage, Stack stack) {
        com.sequenceiq.cloudbreak.cloud.model.Image image = new com.sequenceiq.cloudbreak.cloud.model.Image("imageName",
                Map.of(InstanceGroupType.GATEWAY, "gw user data"),
                statedImage.getImage().getOs(), statedImage.getImage().getOsType(), statedImage.getImage().getArchitecture(),
                statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), statedImage.getImage().getUuid(),
                statedImage.getImage().getPackageVersions(), null, null, null);
        return new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);
    }

    private Component createStackRepoComponent(Stack stack) {
        return new Component(ComponentType.CDH_PRODUCT_DETAILS, ComponentType.CDH_PRODUCT_DETAILS.name(), new Json("{\"dummy\":{}}"), stack);
    }

    private Component createCMComponennt(Stack stack) {
        return new Component(ComponentType.CM_REPO_DETAILS, ComponentType.CM_REPO_DETAILS.name(), new Json(getClouderaManagerRepo()), stack);
    }

    private ClouderaManagerRepo getClouderaManagerRepo() {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("http://public-repo-1.hortonworks.com/cm/centos7/7.2.0/updates/7.2.0");
        clouderaManagerRepo.setVersion(TARGET_STACK_VERSION);
        return clouderaManagerRepo;
    }
}
