package com.sequenceiq.cloudbreak.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UsedImagesProviderTest {

    private static final int THRESHOLD_IN_DAYS = 180;

    @Mock
    private StackService stackService;

    @InjectMocks
    private UsedImagesProvider underTest;

    @Test
    void testEmpty() {
        when(stackService.getImagesOfAliveStacks(THRESHOLD_IN_DAYS)).thenReturn(List.of());

        final UsedImagesListV4Response result = underTest.getUsedImages(THRESHOLD_IN_DAYS);

        assertThat(result.getUsedImages()).isEmpty();
    }

    @Test
    void testSingleImage() {
        when(stackService.getImagesOfAliveStacks(THRESHOLD_IN_DAYS)).thenReturn(List.of(
                createImage("aws-image")));

        final UsedImagesListV4Response result = underTest.getUsedImages(THRESHOLD_IN_DAYS);

        assertThat(result.getUsedImages())
                .hasSize(1)
                .first()
                .matches(usedImage -> usedImage.getNumberOfStacks() == 1);
    }

    @Test
    void testMultipleImages() {
        when(stackService.getImagesOfAliveStacks(THRESHOLD_IN_DAYS)).thenReturn(List.of(
                createImage("aws-image"),
                createImage("aws-image"),
                createImage("azure-image")));

        final UsedImagesListV4Response result = underTest.getUsedImages(THRESHOLD_IN_DAYS);

        assertThat(result.getUsedImages())
                .hasSize(2)
                .anyMatch(usedImage -> usedImage.getImage().getImageId().equals("aws-image") && usedImage.getNumberOfStacks() == 2)
                .anyMatch(usedImage -> usedImage.getImage().getImageId().equals("azure-image") && usedImage.getNumberOfStacks() == 1);
    }

    private Image createImage(String imageId) {
        return new Image("imageName", Map.of(), "os", "osType", "imageCatalogUrl", "imageCatalogName", imageId, null);
    }

}
