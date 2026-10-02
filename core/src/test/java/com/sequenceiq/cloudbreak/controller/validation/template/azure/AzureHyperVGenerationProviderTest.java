package com.sequenceiq.cloudbreak.controller.validation.template.azure;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_TAG;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_V1;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_V2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class AzureHyperVGenerationProviderTest {

    private static final long STACK_ID = 1L;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private AzureHyperVGenerationProvider underTest;

    @Test
    void testGetImageHyperVGenerationWhenGen2TaggedImage() throws Exception {
        setUpImage(Map.of(HYPER_V_GENERATION_TAG, "2"));

        assertEquals(Optional.of(HYPER_V_GENERATION_V2), underTest.getImageHyperVGeneration(azureStack()));
    }

    @Test
    void testGetImageHyperVGenerationWhenGen1TaggedImage() throws Exception {
        setUpImage(Map.of(HYPER_V_GENERATION_TAG, "1"));

        assertEquals(Optional.of(HYPER_V_GENERATION_V1), underTest.getImageHyperVGeneration(azureStack()));
    }

    @Test
    void testGetImageHyperVGenerationWhenImageIsNotTagged() throws Exception {
        setUpImage(Map.of("release-version", "7.3.1"));

        assertTrue(underTest.getImageHyperVGeneration(azureStack()).isEmpty());
    }

    @Test
    void testGetImageHyperVGenerationWhenNotAzureThenImageIsNotEvenRead() throws Exception {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("AWS");

        assertTrue(underTest.getImageHyperVGeneration(stack).isEmpty());

        verify(imageService, never()).getImage(anyLong());
    }

    @Test
    void testGetImageHyperVGenerationWhenImageCannotBeReadThenNoValidation() throws Exception {
        when(imageService.getImage(STACK_ID)).thenThrow(new CloudbreakServiceException("No image found"));

        assertTrue(underTest.getImageHyperVGeneration(azureStack()).isEmpty());
    }

    private void setUpImage(Map<String, String> tags) throws Exception {
        Image image = new Image("image.vhd", Map.of(), "centos7", "redhat7", null, "http://catalog", "catalog", "imageId", Map.of(), null, 0L, tags);
        when(imageService.getImage(STACK_ID)).thenReturn(image);
    }

    private Stack azureStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("AZURE");
        return stack;
    }
}
