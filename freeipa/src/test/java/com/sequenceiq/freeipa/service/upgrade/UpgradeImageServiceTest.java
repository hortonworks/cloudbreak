package com.sequenceiq.freeipa.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class UpgradeImageServiceTest {

    @Mock
    private ImageService imageService;

    @InjectMocks
    private UpgradeImageService underTest;

    @Test
    public void testSelectImage() {
        Stack stack = new Stack();
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("now", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        when(imageService.fetchImageWrapperAndName(stack, imageSettingsRequest)).thenReturn(Pair.of(imageWrapper, "imageName"));

        ImageInfoResponse imageInfoResponse = underTest.selectImage(stack, imageSettingsRequest);
        assertEquals(imageWrapper.getCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageWrapper.getCatalogUrl(), imageInfoResponse.getCatalog());
        assertEquals(image.getDate(), imageInfoResponse.getDate());
        assertEquals(image.getUuid(), imageInfoResponse.getId());
        assertEquals(image.getOs(), imageInfoResponse.getOs());
    }

    @Test
    public void testCurrentImage() {
        Stack stack = new Stack();
        stack.setId(1L);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setImageId("1234-435");
        imageEntity.setImageName("imageName");
        imageEntity.setImageCatalogUrl("catalogUrl");
        imageEntity.setImageCatalogName("catName");
        imageEntity.setOs("linux");
        when(imageService.getByStackId(1L)).thenReturn(imageEntity);

        ImageInfoResponse imageInfoResponse = underTest.currentImage(stack);
        assertEquals(imageEntity.getImageCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageEntity.getImageCatalogUrl(), imageInfoResponse.getCatalog());
        assertNull(imageInfoResponse.getDate());
        assertEquals(imageEntity.getImageId(), imageInfoResponse.getId());
        assertEquals(imageEntity.getOs(), imageInfoResponse.getOs());
    }

}