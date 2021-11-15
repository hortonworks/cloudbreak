package com.sequenceiq.freeipa.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hibernate.envers.AuditReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.ReflectionUtils;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ImageRepository;

@ExtendWith(MockitoExtension.class)
public class ImageServiceTest {

    private static final String DEFAULT_PLATFORM = "aws";

    private static final String DEFAULT_REGION = "eu-west-1";

    private static final String EXISTING_ID = "ami-09fea90f257c85513";

    private static final String FAKE_ID = "fake-ami-0a6931aea1415eb0e";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    private static final String DEFAULT_OS = "redhat7";

    private static final String IMAGE_UUID = "UUID";

    private static final String FREEIPA_VERSION = "2.49.0";

    @Mock
    private ImageProviderFactory imageProviderFactory;

    @Mock
    private ImageProvider imageProvider;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private AuditReader auditReader;

    @InjectMocks
    private ImageService underTest;

    @Mock
    private Image image;

    @Captor
    private ArgumentCaptor<ImageSettingsRequest> imageSettingsRequestCaptor;

    @Test
    public void tesDetermineImageNameFound() {
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(DEFAULT_REGION, EXISTING_ID)));

        String imageName = underTest.determineImageName(DEFAULT_PLATFORM, DEFAULT_REGION, image);
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    public void tesDetermineImageNameNotFound() {
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(DEFAULT_REGION, EXISTING_ID)));

        Exception exception = assertThrows(RuntimeException.class, () ->
                underTest.determineImageName(DEFAULT_PLATFORM, "fake-region", image));
        String exceptionMessage = "Virtual machine image couldn't be found in image";
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void testGetImageGivenIdInputNotFound() {
        ImageSettingsRequest imageSettings = new ImageSettingsRequest();
        imageSettings.setCatalog(IMAGE_CATALOG);
        imageSettings.setId(FAKE_ID);
        imageSettings.setOs(DEFAULT_OS);

        when(imageProviderFactory.getImageProvider(IMAGE_CATALOG)).thenReturn(imageProvider);
        when(imageProvider.getImage(imageSettings, DEFAULT_REGION, DEFAULT_PLATFORM)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                underTest.getImage(imageSettings, DEFAULT_REGION, DEFAULT_PLATFORM));
        String exceptionMessage = "Could not find any image with id: 'fake-ami-0a6931aea1415eb0e' in region 'eu-west-1' with OS 'redhat7'.";
        assertEquals(exceptionMessage, exception.getMessage());
    }

    @Test
    public void testImageChange() {
        Stack stack = new Stack();
        stack.setCloudPlatform(DEFAULT_PLATFORM);
        stack.setRegion(DEFAULT_REGION);
        ImageSettingsRequest imageRequest = new ImageSettingsRequest();
        when(imageProviderFactory.getImageProvider(any())).thenReturn(imageProvider);
        when(imageProvider.getImage(imageRequest, stack.getRegion(), stack.getCloudPlatform()))
                .thenReturn(Optional.of(new ImageWrapper(image, IMAGE_CATALOG_URL, IMAGE_CATALOG)));
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(DEFAULT_REGION, EXISTING_ID)));
        when(imageRepository.getByStack(stack)).thenReturn(new ImageEntity());
        when(image.getUuid()).thenReturn(IMAGE_UUID);
        when(imageRepository.save(any(ImageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0, ImageEntity.class));

        ImageEntity imageEntity = underTest.changeImage(stack, imageRequest);

        assertEquals(EXISTING_ID, imageEntity.getImageName());
        assertEquals(IMAGE_CATALOG_URL, imageEntity.getImageCatalogUrl());
        assertEquals(IMAGE_CATALOG, imageEntity.getImageCatalogName());
        assertEquals(IMAGE_UUID, imageEntity.getImageId());
    }

    @Test
    public void testRevert() {
        ImageEntity originalImage = new ImageEntity();
        originalImage.setImageName(EXISTING_ID);
        originalImage.setImageId(IMAGE_UUID);
        originalImage.setImageCatalogName(IMAGE_CATALOG);
        originalImage.setImageCatalogUrl(IMAGE_CATALOG_URL);
        when(auditReader.find(ImageEntity.class, 2L, 3L)).thenReturn(originalImage);
        ImageEntity currentImage = new ImageEntity();
        currentImage.setId(2L);
        when(imageRepository.findById(2L)).thenReturn(Optional.of(currentImage));

        underTest.revertImageToRevision(2L, 3L);

        ArgumentCaptor<ImageEntity> captor = ArgumentCaptor.forClass(ImageEntity.class);
        verify(imageRepository).save(captor.capture());
        ImageEntity revertedImage = captor.getValue();
        assertEquals(2L, revertedImage.getId());
        assertEquals(IMAGE_UUID, revertedImage.getImageId());
        assertEquals(IMAGE_CATALOG, revertedImage.getImageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, revertedImage.getImageCatalogUrl());
        assertEquals(EXISTING_ID, revertedImage.getImageName());
    }

    @Test
    void testGenerateForStack() throws NoSuchFieldException {
        ReflectionUtils.setField(ImageService.class.getDeclaredField("freeIpaVersion"), underTest, FREEIPA_VERSION);

        Stack stack = new Stack();
        stack.setRegion(DEFAULT_REGION);
        stack.setCloudPlatform(DEFAULT_PLATFORM);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setImageId(IMAGE_UUID);
        imageEntity.setOs(DEFAULT_OS);
        imageEntity.setImageCatalogName(IMAGE_CATALOG);
        imageEntity.setImageCatalogUrl(IMAGE_CATALOG_URL);
        when(imageRepository.getByStack(stack)).thenReturn(imageEntity);

        when(imageProviderFactory.getImageProvider(IMAGE_CATALOG)).thenReturn(imageProvider);
        Image image = new Image(123L, "now", "desc", DEFAULT_OS, IMAGE_UUID, Map.of(), "os", Map.of(), true);
        ImageWrapper imageWrapper = new ImageWrapper(image, IMAGE_CATALOG_URL, IMAGE_CATALOG);
        when(imageProvider.getImage(any(), any(), any())).thenReturn(Optional.of(imageWrapper));

        ImageCatalog result = underTest.generateImageCatalogForStack(stack);

        verify(imageProvider).getImage(imageSettingsRequestCaptor.capture(), eq(DEFAULT_REGION), eq(DEFAULT_PLATFORM));
        assertThat(imageSettingsRequestCaptor.getValue())
                .returns(IMAGE_CATALOG, ImageSettingsBase::getCatalog)
                .returns(IMAGE_UUID, ImageSettingsBase::getId);

        assertThat(result.getImages().getFreeipaImages())
                .containsExactly(image);
        assertThat(result.getVersions()).isNull();
    }
}
