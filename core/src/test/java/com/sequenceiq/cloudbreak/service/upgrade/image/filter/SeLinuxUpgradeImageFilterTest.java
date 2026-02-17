package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;

@ExtendWith(MockitoExtension.class)
class SeLinuxUpgradeImageFilterTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private SeLinuxValidationService seLinuxValidationService;

    @InjectMocks
    private SeLinuxUpgradeImageFilter underTest;

    @Test
    void testFilterWhenEntitlementNotGranted() {
        ImageFilterResult input = new ImageFilterResult(List.of(
                Image.builder().withUuid("image-1").build(),
                Image.builder().withUuid("image-2").build(),
                Image.builder().withUuid("image-3").build()
        ));
        ImageFilterParams imageFilterParams = ImageFilterParams.builder()
                .withStackId(STACK_ID)
                .build();
        StackDto stack = mock();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxEntitlementGranted(stack);

        ImageFilterResult result = underTest.filter(input, imageFilterParams);

        assertThat(result.getImages()).isEmpty();
        assertEquals("SeLinux validation filtered out some of the potential images.", result.getReason());
        verify(seLinuxValidationService, times(3)).validateSeLinuxEntitlementGranted(stack);
        verify(seLinuxValidationService, never()).validateSeLinuxSupportedOnTargetImage(eq(stack), any(Image.class));
    }

    @Test
    void testFilterWhenSomeImagesDontSupportSeLinux() {
        Image image1 = Image.builder().withUuid("image-1").build();
        Image image2 = Image.builder().withUuid("image-2").build();
        Image image3 = Image.builder().withUuid("image-3").build();
        ImageFilterResult input = new ImageFilterResult(List.of(image1, image2, image3));
        ImageFilterParams imageFilterParams = ImageFilterParams.builder()
                .withStackId(STACK_ID)
                .build();
        StackDto stack = mock();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        doNothing().when(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(stack, image1);
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(stack, image2);
        doNothing().when(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(stack, image3);

        ImageFilterResult result = underTest.filter(input, imageFilterParams);

        assertThat(result.getImages()).containsExactlyInAnyOrder(image1, image3);
        assertEquals(ImageFilterResult.EMPTY_REASON, result.getReason());
        verify(seLinuxValidationService, times(3)).validateSeLinuxEntitlementGranted(stack);
        verify(seLinuxValidationService, times(3)).validateSeLinuxSupportedOnTargetImage(eq(stack), any(Image.class));
    }

    @Test
    void testFilterWhenTargetImageDoesNotSupportSeLinux() {
        Image targetImage = Image.builder().withUuid("target-image").build();
        ImageFilterResult input = new ImageFilterResult(List.of(targetImage));
        ImageFilterParams imageFilterParams = ImageFilterParams.builder()
                .withStackId(STACK_ID)
                .withCurrentImage(com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                        .withImageId("current-image")
                        .build())
                .withTargetImageId("target-image")
                .build();
        StackDto stack = mock();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        doNothing().when(seLinuxValidationService).validateSeLinuxEntitlementGranted(stack);
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(stack, targetImage);

        ImageFilterResult result = underTest.filter(input, imageFilterParams);

        assertThat(result.getImages()).isEmpty();
        assertEquals("Can't upgrade to 'target-image' image from 'current-image' image. " +
                "SeLinux validation filtered out the target image 'target-image'.", result.getReason());
        verify(seLinuxValidationService).validateSeLinuxEntitlementGranted(stack);
        verify(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(stack, targetImage);
    }

    @Test
    void testGetFilterOrderNumber() {
        assertEquals(11, underTest.getFilterOrderNumber());
    }
}
