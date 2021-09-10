package com.sequenceiq.freeipa.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
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

        ImageInfoResponse imageInfoResponse = underTest.fetchCurrentImage(stack);

        assertEquals(imageEntity.getImageCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageEntity.getImageCatalogUrl(), imageInfoResponse.getCatalog());
        assertNull(imageInfoResponse.getDate());
        assertEquals(imageEntity.getImageId(), imageInfoResponse.getId());
        assertEquals(imageEntity.getOs(), imageInfoResponse.getOs());
    }

    @Test
    public void testFindTargetImages() {
        Stack stack = new Stack();
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("2021-09-01", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        when(imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest)).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("111-222");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, imageSettingsRequest, currentImage);

        assertEquals(1, targetImages.size());
        ImageInfoResponse imageInfoResponse = targetImages.get(0);
        assertEquals(imageWrapper.getCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageWrapper.getCatalogUrl(), imageInfoResponse.getCatalog());
        assertEquals(image.getDate(), imageInfoResponse.getDate());
        assertEquals(image.getUuid(), imageInfoResponse.getId());
        assertEquals(image.getOs(), imageInfoResponse.getOs());
    }

    @Test
    public void testFindTargetImagesNoNewerImage() {
        Stack stack = new Stack();
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("2021-07-01", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        when(imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest)).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("111-222");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, imageSettingsRequest, currentImage);

        assertTrue(targetImages.isEmpty());
    }

    @Test
    public void testFindTargetImagesImageWithSameId() {
        Stack stack = new Stack();
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("2021-09-01", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        when(imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest)).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("1234-456");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, imageSettingsRequest, currentImage);

        assertTrue(targetImages.isEmpty());
    }

    @Test
    public void testFindTargetImagesCurrentImageMissingDate() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setRegion("reg");
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("2021-09-01", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        when(imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest)).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));
        ArgumentCaptor<ImageSettingsRequest> captor = ArgumentCaptor.forClass(ImageSettingsRequest.class);
        Image currentImageFromCatalog = new Image("2021-08-01", "desc", "linux", "222-333", Map.of(), "magicOs");
        ImageWrapper currentImageWrapperFromCatalog = new ImageWrapper(currentImageFromCatalog, "asdf", "Asdf");
        when(imageService.getImage(captor.capture(), eq(stack.getRegion()), eq(stack.getCloudPlatform().toLowerCase())))
                .thenReturn(currentImageWrapperFromCatalog);

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("222-333");
        currentImage.setCatalog("cat");
        currentImage.setCatalogName("catName");
        currentImage.setOs("zOs");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, imageSettingsRequest, currentImage);

        assertEquals(1, targetImages.size());
        ImageInfoResponse imageInfoResponse = targetImages.get(0);
        assertEquals(imageWrapper.getCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageWrapper.getCatalogUrl(), imageInfoResponse.getCatalog());
        assertEquals(image.getDate(), imageInfoResponse.getDate());
        assertEquals(image.getUuid(), imageInfoResponse.getId());
        assertEquals(image.getOs(), imageInfoResponse.getOs());
        ImageSettingsRequest settingsRequest = captor.getValue();
        assertEquals(currentImage.getCatalog(), settingsRequest.getCatalog());
        assertEquals(currentImage.getId(), settingsRequest.getId());
        assertEquals(currentImage.getOs(), settingsRequest.getOs());
    }

    @Test
    public void testFindTargetImagesCurrentImageMissingDateAndNoDateFromCatalog() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setRegion("reg");
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("2021-09-01", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        when(imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest)).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));
        ArgumentCaptor<ImageSettingsRequest> captor = ArgumentCaptor.forClass(ImageSettingsRequest.class);
        Image currentImageFromCatalog = new Image(null, "desc", "linux", "222-333", Map.of(), "magicOs");
        ImageWrapper currentImageWrapperFromCatalog = new ImageWrapper(currentImageFromCatalog, "asdf", "Asdf");
        when(imageService.getImage(captor.capture(), eq(stack.getRegion()), eq(stack.getCloudPlatform().toLowerCase())))
                .thenReturn(currentImageWrapperFromCatalog);

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("222-333");
        currentImage.setCatalogName("cat");
        currentImage.setOs("zOs");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, imageSettingsRequest, currentImage);

        assertTrue(targetImages.isEmpty());
        ImageSettingsRequest settingsRequest = captor.getValue();
        assertEquals(currentImage.getCatalogName(), settingsRequest.getCatalog());
        assertEquals(currentImage.getId(), settingsRequest.getId());
        assertEquals(currentImage.getOs(), settingsRequest.getOs());
    }

    @Test
    public void testFindTargetImagesCurrentImageMissingDateAndNoImageFoundInCatalog() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setRegion("reg");
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("2021-09-01", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        when(imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest)).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));
        ArgumentCaptor<ImageSettingsRequest> captor = ArgumentCaptor.forClass(ImageSettingsRequest.class);
        when(imageService.getImage(captor.capture(), eq(stack.getRegion()), eq(stack.getCloudPlatform().toLowerCase())))
                .thenThrow(new ImageNotFoundException("Image not found"));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("222-333");
        currentImage.setCatalogName("cat");
        currentImage.setOs("zOs");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, imageSettingsRequest, currentImage);

        assertTrue(targetImages.isEmpty());
        ImageSettingsRequest settingsRequest = captor.getValue();
        assertEquals(currentImage.getCatalogName(), settingsRequest.getCatalog());
        assertEquals(currentImage.getId(), settingsRequest.getId());
        assertEquals(currentImage.getOs(), settingsRequest.getOs());
    }

    @Test
    public void testFindTargetImagesImageWithWrongDateFormatIgnored() {
        Stack stack = new Stack();
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        Image image = new Image("2021-09-01", "desc", "linux", "1234-456", Map.of(), "magicOs");
        ImageWrapper imageWrapper = new ImageWrapper(image, "catalogURL", "catalogName");
        Image image2 = new Image("20210901", "desc", "linux", "1234-789", Map.of(), "magicOs");
        ImageWrapper imageWrapper2 = new ImageWrapper(image2, "catalogURL", "catalogName");
        when(imageService.fetchImagesWrapperAndName(stack, imageSettingsRequest))
                .thenReturn(List.of(Pair.of(imageWrapper, "imageName"), Pair.of(imageWrapper2, "imageName2")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("111-222");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, imageSettingsRequest, currentImage);

        assertEquals(1, targetImages.size());
        ImageInfoResponse imageInfoResponse = targetImages.get(0);
        assertEquals(imageWrapper.getCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageWrapper.getCatalogUrl(), imageInfoResponse.getCatalog());
        assertEquals(image.getDate(), imageInfoResponse.getDate());
        assertEquals(image.getUuid(), imageInfoResponse.getId());
        assertEquals(image.getOs(), imageInfoResponse.getOs());
    }
}