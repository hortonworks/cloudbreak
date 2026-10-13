package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.javaversion.JavaVersionPolicy;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class SetDefaultJavaVersionFlowChainServiceTest {

    private static final String IMAGE_ID = "imageId";

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 2L;

    private static final String IMAGE_CATALOG_NAME = "dev";

    private static final String IMAGE_CATALOG_URL = "http://dev.catalog.url";

    private static final long CLUSTER_ID = 3L;

    private static final String SOURCE_RUNTIME_VERSION = "7.3.1.600";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private JavaVersionPolicy javaVersionPolicy;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @InjectMocks
    private SetDefaultJavaVersionFlowChainService underTest;

    @Test
    void aTriggerEventIsCreatedWhenThePolicyReturnsAnAutoUpgradeTarget() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setUpImage("7.3.2");
        when(javaVersionPolicy.autoUpgradeTarget(SOURCE_RUNTIME_VERSION, "7.3.2", 8)).thenReturn(OptionalInt.of(17));

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto(8),
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));

        assertThat(events).hasSize(1);
        SetDefaultJavaVersionTriggerEvent javaEvent = events.getFirst();
        assertEquals(SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT, javaEvent.selector());
        assertEquals(STACK_ID, javaEvent.getResourceId());
        assertEquals("17", javaEvent.getDefaultJavaVersion());
    }

    @Test
    void noTriggerEventIsCreatedWhenThePolicyKeepsTheCurrentJavaVersion() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setUpImage("7.3.3.0");
        when(javaVersionPolicy.autoUpgradeTarget(SOURCE_RUNTIME_VERSION, "7.3.3.0", 17)).thenReturn(OptionalInt.empty());

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto(17),
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));

        assertThat(events).isEmpty();
    }

    @Test
    void anImageWithoutRuntimeVersionIsPassedToThePolicyAsAnEmptyVersion() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage statedImage = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.empty());
        when(statedImage.getImage()).thenReturn(image);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(statedImage);
        when(javaVersionPolicy.autoUpgradeTarget(SOURCE_RUNTIME_VERSION, "", 17)).thenReturn(OptionalInt.empty());

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto(17),
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));

        assertThat(events).isEmpty();
    }

    @Test
    void theRuntimeTheClusterIsUpgradedFromIsPassedToThePolicy() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setUpImage("7.3.3.0");
        when(javaVersionPolicy.autoUpgradeTarget(SOURCE_RUNTIME_VERSION, "7.3.3.0", 17)).thenReturn(OptionalInt.of(21));

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto(17),
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));

        assertThat(events).singleElement().satisfies(event -> assertEquals("21", event.getDefaultJavaVersion()));
        verify(runtimeVersionService).getServicePackQualifiedRuntimeVersion(CLUSTER_ID);
    }

    @Test
    void anUnknownSourceRuntimeIsPassedToThePolicyAsAnEmptyVersion() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setUpImage("7.3.3.0");
        StackDto stackDto = stackDto(17);
        when(runtimeVersionService.getServicePackQualifiedRuntimeVersion(CLUSTER_ID)).thenReturn(Optional.empty());
        when(javaVersionPolicy.autoUpgradeTarget("", "7.3.3.0", 17)).thenReturn(OptionalInt.empty());

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto,
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));

        assertThat(events).isEmpty();
    }

    @Test
    void aMissingImageIsReportedAsNotFound() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID))
                .thenThrow(new CloudbreakImageNotFoundException("missing"));
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getWorkspaceId()).thenReturn(WORKSPACE_ID);

        assertThatThrownBy(() -> underTest.setDefaultJavaVersionTriggerEvent(stackDto,
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Image not found in image catalog");
    }

    private void setUpImage(String targetRuntimeVersion) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage statedImage = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.getRuntimeVersion()).thenReturn(Optional.of(targetRuntimeVersion));
        when(statedImage.getImage()).thenReturn(image);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(statedImage);
    }

    private StackDto stackDto(int currentJavaVersion) {
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        Stack stack = mock(Stack.class);
        when(stack.getJavaVersion()).thenReturn(currentJavaVersion);
        when(stackDto.getStack()).thenReturn(stack);
        ClusterView cluster = mock(ClusterView.class);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(runtimeVersionService.getServicePackQualifiedRuntimeVersion(CLUSTER_ID)).thenReturn(Optional.of(SOURCE_RUNTIME_VERSION));
        return stackDto;
    }
}
