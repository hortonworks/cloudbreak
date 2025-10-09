package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.java.vm.AllowableJavaUpdateConfigurations;

@ExtendWith(MockitoExtension.class)
class SetDefaultJavaVersionFlowChainServiceTest {

    private static final String IMAGE_ID = "imageId";

    private static final long STACK_ID = 1L;

    private static final String IMAGE_CATALOG_NAME = "dev";

    private static final String IMAGE_CATALOG_URL = "http://dev.catalog.url";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private AllowableJavaUpdateConfigurations allowableJavaUpdateConfigurations;

    @InjectMocks
    private SetDefaultJavaVersionFlowChainService underTest;

    @Test
    void testCreateFlowTriggerEventQueueWithSetDefaultJava()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(1L);
        when(stackDto.getWorkspaceId()).thenReturn(1L);
        Stack stack = mock(Stack.class);
        when(stack.getJavaVersion()).thenReturn(8);
        when(stackDto.getStack()).thenReturn(stack);
        StatedImage statedImage = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.getPackageVersion(ImagePackageVersion.STACK)).thenReturn("7.3.2");
        when(statedImage.getImage()).thenReturn(image);
        when(imageCatalogService.getImage(STACK_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID))
                .thenReturn(statedImage);
        when(entitlementService.isAutoJavaUpgaradeEnabled(any())).thenReturn(true);
        when(allowableJavaUpdateConfigurations.getMinJavaVersionForRuntime("7.3.2")).thenReturn(17);

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto,
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));

        SetDefaultJavaVersionTriggerEvent javaEvent = events.get(0);
        assertEquals(SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT, javaEvent.selector());
        assertEquals(STACK_ID, javaEvent.getResourceId());
        assertEquals("17", javaEvent.getDefaultJavaVersion());
        assertEquals(1, javaEvent.getResourceId());

        when(entitlementService.isAutoJavaUpgaradeEnabled(any())).thenReturn(false);
        events = underTest.setDefaultJavaVersionTriggerEvent(stackDto,
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));
        assertTrue(events.isEmpty());
    }

    @Test
    void testDontCreateFlowTriggerEventQueueWithSetDefaultJava()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getWorkspaceId()).thenReturn(1L);
        Stack stack = mock(Stack.class);
        when(stack.getJavaVersion()).thenReturn(8);
        when(stackDto.getStack()).thenReturn(stack);
        StatedImage statedImage = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(image.getPackageVersion(ImagePackageVersion.STACK)).thenReturn("7.3.1");
        when(statedImage.getImage()).thenReturn(image);
        when(imageCatalogService.getImage(STACK_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID))
                .thenReturn(statedImage);
        when(entitlementService.isAutoJavaUpgaradeEnabled(any())).thenReturn(true);
        when(allowableJavaUpdateConfigurations.getMinJavaVersionForRuntime("7.3.1")).thenReturn(8);

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto,
                new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL));

        assertTrue(events.isEmpty());
    }
}