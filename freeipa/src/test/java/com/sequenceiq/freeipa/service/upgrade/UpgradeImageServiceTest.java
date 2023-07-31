package com.sequenceiq.freeipa.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class UpgradeImageServiceTest {

    private static final String CATALOG_URL = "catalogURL";

    @Mock
    private FreeIpaImageFilterSettings imageFilterSettings;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private UpgradeImageService underTest;

    @Test
    public void testSelectImage() {
        Image image = createImage("now");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        when(imageService.fetchImageWrapperAndName(imageFilterSettings)).thenReturn(Pair.of(imageWrapper, "imageName"));

        ImageInfoResponse imageInfoResponse = underTest.selectImage(imageFilterSettings);

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
        Image image = createImage("2021-09-01");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        when(imageService.fetchImagesWrapperAndName(eq(stack), any(), any(), eq(true))).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("111-222");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, CATALOG_URL, currentImage, true);

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
        Image image = createImage("2021-07-01");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        when(imageService.fetchImagesWrapperAndName(eq(stack), any(), any(), eq(true))).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("111-222");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, CATALOG_URL, currentImage, true);

        assertTrue(targetImages.isEmpty());
    }

    @Test
    public void testFindTargetImagesImageWithSameId() {
        Stack stack = new Stack();
        Image image = createImage("2021-09-01");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        when(imageService.fetchImagesWrapperAndName(eq(stack), any(), any(), eq(true))).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("1234-456");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, CATALOG_URL, currentImage, true);

        assertTrue(targetImages.isEmpty());
    }

    @Test
    public void testFindTargetImagesCurrentImageMissingDate() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setRegion("reg");
        Image image = createImage("2021-09-01");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        when(imageService.fetchImagesWrapperAndName(eq(stack), any(), any(), eq(true))).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));
        ArgumentCaptor<FreeIpaImageFilterSettings> captor = ArgumentCaptor.forClass(FreeIpaImageFilterSettings.class);
        Image currentImageFromCatalog = createImage("2021-08-01");
        ImageWrapper currentImageWrapperFromCatalog = new ImageWrapper(currentImageFromCatalog, "asdf", "Asdf");
        when(imageService.getImage(captor.capture())).thenReturn(currentImageWrapperFromCatalog);

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("222-333");
        currentImage.setCatalog("cat");
        currentImage.setCatalogName("catName");
        currentImage.setOs("zOs");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, CATALOG_URL, currentImage, true);

        assertEquals(1, targetImages.size());
        ImageInfoResponse imageInfoResponse = targetImages.get(0);
        assertEquals(imageWrapper.getCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageWrapper.getCatalogUrl(), imageInfoResponse.getCatalog());
        assertEquals(image.getDate(), imageInfoResponse.getDate());
        assertEquals(image.getUuid(), imageInfoResponse.getId());
        assertEquals(image.getOs(), imageInfoResponse.getOs());
        FreeIpaImageFilterSettings settingsRequest = captor.getValue();
        assertEquals(currentImage.getCatalog(), settingsRequest.catalog());
        assertEquals(currentImage.getId(), settingsRequest.currentImageId());
        assertEquals(currentImage.getOs(), settingsRequest.currentOs());
        assertEquals(stack.getRegion(), settingsRequest.region());
        assertEquals(stack.getCloudPlatform().toLowerCase(), settingsRequest.platform());
    }

    @Test
    public void testFindTargetImagesCurrentImageMissingDateAndNoDateFromCatalog() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setRegion("reg");
        Image image = createImage("2021-09-01");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        when(imageService.fetchImagesWrapperAndName(eq(stack), any(), any(), eq(true))).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));
        ArgumentCaptor<FreeIpaImageFilterSettings> captor = ArgumentCaptor.forClass(FreeIpaImageFilterSettings.class);
        Image currentImageFromCatalog = createImage(null);
        ImageWrapper currentImageWrapperFromCatalog = new ImageWrapper(currentImageFromCatalog, "asdf", "Asdf");
        when(imageService.getImage(captor.capture())).thenReturn(currentImageWrapperFromCatalog);

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("222-333");
        currentImage.setCatalogName("cat");
        currentImage.setOs("zOs");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, CATALOG_URL, currentImage, true);

        assertTrue(targetImages.isEmpty());
        FreeIpaImageFilterSettings settingsRequest = captor.getValue();
        assertEquals(currentImage.getCatalogName(), settingsRequest.catalog());
        assertEquals(currentImage.getId(), settingsRequest.currentImageId());
        assertEquals(currentImage.getOs(), settingsRequest.currentOs());
        assertEquals(stack.getRegion(), settingsRequest.region());
        assertEquals(stack.getCloudPlatform().toLowerCase(), settingsRequest.platform());
    }

    @Test
    public void testFindTargetImagesCurrentImageMissingDateAndNoImageFoundInCatalog() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setRegion("reg");
        Image image = createImage("2021-09-01");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        when(imageService.fetchImagesWrapperAndName(eq(stack), any(), any(), eq(true))).thenReturn(List.of(Pair.of(imageWrapper, "imageName")));
        ArgumentCaptor<FreeIpaImageFilterSettings> captor = ArgumentCaptor.forClass(FreeIpaImageFilterSettings.class);
        when(imageService.getImage(captor.capture())).thenThrow(new ImageNotFoundException("Image not found"));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("222-333");
        currentImage.setCatalogName("cat");
        currentImage.setOs("zOs");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, CATALOG_URL, currentImage, true);

        assertTrue(targetImages.isEmpty());
        FreeIpaImageFilterSettings settingsRequest = captor.getValue();
        assertEquals(currentImage.getCatalogName(), settingsRequest.catalog());
        assertEquals(currentImage.getId(), settingsRequest.currentImageId());
        assertEquals(currentImage.getOs(), settingsRequest.currentOs());
        assertEquals(stack.getRegion(), settingsRequest.region());
        assertEquals(stack.getCloudPlatform().toLowerCase(), settingsRequest.platform());
    }

    @Test
    public void testFindTargetImagesImageWithWrongDateFormatIgnored() {
        Stack stack = new Stack();
        Image image = createImage("2021-09-01");
        ImageWrapper imageWrapper = new ImageWrapper(image, CATALOG_URL, "catalogName");
        Image image2 = createImage("20210901");
        ImageWrapper imageWrapper2 = new ImageWrapper(image2, CATALOG_URL, "catalogName");
        when(imageService.fetchImagesWrapperAndName(eq(stack), any(), any(), eq(true)))
                .thenReturn(List.of(Pair.of(imageWrapper, "imageName"), Pair.of(imageWrapper2, "imageName2")));

        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setDate("2021-08-21");
        currentImage.setId("111-222");

        List<ImageInfoResponse> targetImages = underTest.findTargetImages(stack, CATALOG_URL, currentImage, true);

        assertEquals(1, targetImages.size());
        ImageInfoResponse imageInfoResponse = targetImages.get(0);
        assertEquals(imageWrapper.getCatalogName(), imageInfoResponse.getCatalogName());
        assertEquals(imageWrapper.getCatalogUrl(), imageInfoResponse.getCatalog());
        assertEquals(image.getDate(), imageInfoResponse.getDate());
        assertEquals(image.getUuid(), imageInfoResponse.getId());
        assertEquals(image.getOs(), imageInfoResponse.getOs());
    }

    private Image createImage(String date) {
        return new Image(123L, date, "desc", "linux", "1234-456", Map.of(), "magicOs", Map.of(), false);
    }
}