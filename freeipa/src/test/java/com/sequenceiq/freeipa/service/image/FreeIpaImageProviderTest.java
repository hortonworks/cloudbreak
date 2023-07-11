package com.sequenceiq.freeipa.service.image;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.dto.ImageWrapper;

@ExtendWith(MockitoExtension.class)
public class FreeIpaImageProviderTest {

    private static final String DEFAULT_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/freeipa-image-catalog.json";

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final String CATALOG_FILE = "com/sequenceiq/freeipa/service/image/freeipa-catalog-1.json";

    private static final String DEFAULT_OS = "redhat7";

    private static final String DEFAULT_PLATFORM = "aws";

    private static final String DEFAULT_REGION = "eu-west-1";

    private static final String LATEST_DATE = "2019-05-06";

    private static final String LATEST_DATE_NO_INPUT = "2019-05-07";

    private static final String EXISTING_ID = "ami-09fea90f257c85513";

    private static final String NON_EXISTING_ID = "fake-ami-0a6931aea1415eb0e";

    private static final String NON_EXISTING_OS = "Ubuntu7";

    private static final String DEFAULT_VERSION = "2.20.0-dev.1";

    private static final String IMAGE_UUID = "61851893-8340-411d-afb7-e1b55107fb10";

    private static final String NON_DEFAULT_OS_IMAGE_UUID = "91851893-8340-411d-afb7-e1b55107fb10";

    private static final String REDHAT8_OS_IMAGE_UUID = "b465c893-fe04-44b1-ae8e-0452bbb39c99";

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Mock
    private FreeIpaImageFilter freeIpaImageFilter;

    @InjectMocks
    private FreeIpaImageProvider underTest;

    @Spy
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() throws Exception {
        setupImageCatalogProvider();
        lenient().when(providerSpecificImageFilter.filterImages(any(), anyList())).then(returnsSecondArg());
        lenient().when(freeIpaImageFilter.filterImages(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(freeIpaImageFilter.filterRegion(any(), any())).thenReturn(true);

        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "defaultOs", DEFAULT_OS, null);
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "defaultCatalogUrl", DEFAULT_CATALOG_URL, null);
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "freeIpaVersion", DEFAULT_VERSION, null);
    }

    @Test
    public void testGetImageGivenNoInputWithGbnAppVersion() {
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "freeIpaVersion", "2.21.0-b1", null);
        doTestGetImageGivenNoInput();
    }

    @Test
    public void testGetImageGivenNoInputWithVersionNotInCatalog() {
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "freeIpaVersion", "2.20.0-dev.2", null);
        doTestGetImageGivenNoInput();
    }

    @Test
    public void testGetImagesGivenNoInputWithInvalidAppVersion() {
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "freeIpaVersion", "2.21.0-dcv.1", null);
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(null, null, "centos7", false);
        when(freeIpaImageFilter.filterImages(any(), any())).thenReturn(Collections.emptyList());

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertTrue(images.isEmpty());
    }

    private void doTestGetImageGivenNoInput() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(null, null, null, false);

        Image image = underTest.getImage(imageFilterSettings).get().getImage();

        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE_NO_INPUT, image.getDate());
        assertEquals("71851893-8340-411d-afb7-e1b55107fb10", image.getUuid());
    }

    @Test
    public void testGetImageGivenAllInput() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(EXISTING_ID, CUSTOM_IMAGE_CATALOG_URL, DEFAULT_OS, false);
        //when(freeIpaImageFilter.filterRegion(any(), any())).thenReturn(true);

        Image image = underTest.getImage(imageFilterSettings).get().getImage();

        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImagesGivenAllInput() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(EXISTING_ID, CUSTOM_IMAGE_CATALOG_URL, DEFAULT_OS, false);

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertEquals(1, images.size());
        ImageWrapper imageWrapper = images.get(0);
        assertEquals(CUSTOM_IMAGE_CATALOG_URL, imageWrapper.getCatalogUrl());
        assertNull(imageWrapper.getCatalogName());
        Image image = imageWrapper.getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImagesWhenMajorOsUpgradeIsEnabled() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(null, CUSTOM_IMAGE_CATALOG_URL, DEFAULT_OS, true);

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertTrue(images.stream().anyMatch(imageWrapper -> imageWrapper.getImage().getUuid().equals(REDHAT8_OS_IMAGE_UUID)));
    }

    @Test
    public void testGetImageGivenAllInputNonExistentOS() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(EXISTING_ID, CUSTOM_IMAGE_CATALOG_URL, NON_EXISTING_OS, false);

        Image image = underTest.getImage(imageFilterSettings).get().getImage();

        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImagesGivenAllInputNonExistentOS() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(EXISTING_ID, CUSTOM_IMAGE_CATALOG_URL, NON_EXISTING_OS, false);

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertEquals(1, images.size());
        ImageWrapper imageWrapper = images.get(0);
        assertEquals(CUSTOM_IMAGE_CATALOG_URL, imageWrapper.getCatalogUrl());
        assertNull(imageWrapper.getCatalogName());
        Image image = imageWrapper.getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImageGivenIdInputFound() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(EXISTING_ID, null, null, false);

        Image image = underTest.getImage(imageFilterSettings).get().getImage();

        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImagesGivenIdInputFound() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(EXISTING_ID, null, null, false);
        //when(freeIpaImageFilter.filterImages())

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertEquals(1, images.size());
        ImageWrapper imageWrapper = images.get(0);
        assertEquals(DEFAULT_CATALOG_URL, imageWrapper.getCatalogUrl());
        assertNull(imageWrapper.getCatalogName());
        Image image = imageWrapper.getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImageGivenUuidInputFound() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(IMAGE_UUID, null, null, false);

        Image image = underTest.getImage(imageFilterSettings).get().getImage();

        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImagesGivenUuidInputFound() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(IMAGE_UUID, null, null, false);

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertEquals(1, images.size());
        ImageWrapper imageWrapper = images.get(0);
        assertEquals(DEFAULT_CATALOG_URL, imageWrapper.getCatalogUrl());
        assertNull(imageWrapper.getCatalogName());
        Image image = imageWrapper.getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals(LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImageGivenIdInputNotFound() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(NON_EXISTING_ID, null, null, false);

        Optional<ImageWrapper> result = underTest.getImage(imageFilterSettings);

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetImagesGivenIdInputNotFound() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(NON_EXISTING_ID, null, null, false);

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertTrue(images.isEmpty());
    }

    @Test
    public void testGetImageGivenUuidInputFoundWithNotDefaultOs() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(NON_DEFAULT_OS_IMAGE_UUID, null, null, false);

        Image image = underTest.getImage(imageFilterSettings).get().getImage();

        assertEquals(NON_DEFAULT_OS_IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImagesGivenUuidInputNotFoundWithNotDefaultOs() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(NON_DEFAULT_OS_IMAGE_UUID, null, null, false);

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertTrue(images.isEmpty());
    }

    @Test
    public void testGetImagesNoInput() {
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(null, null, null, false);

        List<ImageWrapper> images = underTest.getImages(imageFilterSettings);

        assertEquals(3, images.size());
        assertThat(images, hasItem(allOf(
                hasProperty("image", allOf(
                        hasProperty("uuid", is(IMAGE_UUID)),
                        hasProperty("date", is(LATEST_DATE)),
                        hasProperty("os", is(DEFAULT_OS))
                ))
        )));
        assertThat(images, hasItem(allOf(
                hasProperty("image", allOf(
                        hasProperty("uuid", is("71851893-8340-411d-afb7-e1b55107fb10")),
                        hasProperty("date", is(LATEST_DATE_NO_INPUT)),
                        hasProperty("os", is(DEFAULT_OS))
                ))
        )));
    }

    private void setupImageCatalogProvider() throws IOException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(CATALOG_FILE);
        ImageCatalog catalog = objectMapper.readValue(catalogJson, ImageCatalog.class);
        lenient().when(imageCatalogProvider.getImageCatalog(CUSTOM_IMAGE_CATALOG_URL)).thenReturn(catalog);
        lenient().when(imageCatalogProvider.getImageCatalog(DEFAULT_CATALOG_URL)).thenReturn(catalog);
    }

    private FreeIpaImageFilterSettings createImageFilterSettings(String id, String catalog, String os, boolean allowMajorOsUpgrade) {
        return new FreeIpaImageFilterSettings(id, catalog, os, DEFAULT_REGION, DEFAULT_PLATFORM, allowMajorOsUpgrade);
    }
}