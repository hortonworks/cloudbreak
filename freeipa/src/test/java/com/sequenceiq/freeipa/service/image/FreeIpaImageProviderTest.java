package com.sequenceiq.freeipa.service.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.model.image.ImageCatalog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @InjectMocks
    private FreeIpaImageProvider underTest;

    @Spy
    private ObjectMapper objectMapper;

    @Before
    public void setup() throws Exception {
        ImageCatalog imageCatalog = setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, CATALOG_FILE);
        imageCatalog.getImages().getFreeipaImages().get(0);

        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "defaultOs", DEFAULT_OS, null);
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "defaultCatalogUrl", DEFAULT_CATALOG_URL, null);
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "freeIpaVersion", DEFAULT_VERSION, null);
    }

    @Test
    public void testGetImageGivenNoInputWithInvalidAppVersion() {
        ReflectionTestUtils.setField(underTest, FreeIpaImageProvider.class, "freeIpaVersion", "2.21.0-dcv.1", null);
        ImageSettingsRequest is = setupImageSettingsRequest(null, null, "centos7");
        Image image = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM).get().getImage();
        assertEquals("centos7", image.getOs());
        assertEquals("Assuming the latest image to be selected", "2019-05-09", image.getDate());
        assertEquals("91851893-8340-411d-afb7-e1b55107fb10", image.getUuid());
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

    private void doTestGetImageGivenNoInput() {
        ImageSettingsRequest is = setupImageSettingsRequest(null, null, null);
        Image image = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM).get().getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals("Assuming the latest image to be selected", LATEST_DATE_NO_INPUT, image.getDate());
        assertEquals("71851893-8340-411d-afb7-e1b55107fb10", image.getUuid());
    }

    @Test
    public void testGetImageGivenAllInput() {
        ImageSettingsRequest is = setupImageSettingsRequest(EXISTING_ID, CUSTOM_IMAGE_CATALOG_URL, DEFAULT_OS);
        Image image = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM).get().getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals("Assuming the latest image to be selected", LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImageGivenAllInputNonExistentOS() {
        ImageSettingsRequest is = setupImageSettingsRequest(EXISTING_ID, CUSTOM_IMAGE_CATALOG_URL, NON_EXISTING_OS);
        Image image = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM).get().getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals("Assuming the latest image to be selected", LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImageGivenIdInputFound() {
        ImageSettingsRequest is = setupImageSettingsRequest(EXISTING_ID, null, null);
        Image image = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM).get().getImage();
        assertEquals(DEFAULT_OS, image.getOs());
        assertEquals("Assuming the latest image to be selected", LATEST_DATE, image.getDate());
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImageGivenUuidInputFound() {
        ImageSettingsRequest is = setupImageSettingsRequest(IMAGE_UUID, null, null);
        Image image = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM).get().getImage();
        assertEquals(IMAGE_UUID, image.getUuid());
    }

    @Test
    public void testGetImageGivenIdInputNotFound() {
        ImageSettingsRequest is = setupImageSettingsRequest(NON_EXISTING_ID, null, null);

        Optional<ImageWrapper> result = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM);
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetImageGivenUuidInputFoundWithNotDefaultOs() {
        ImageSettingsRequest is = setupImageSettingsRequest(NON_DEFAULT_OS_IMAGE_UUID, null, null);
        Image image = underTest.getImage(is, DEFAULT_REGION, DEFAULT_PLATFORM).get().getImage();
        assertEquals(NON_DEFAULT_OS_IMAGE_UUID, image.getUuid());
    }

    private ImageCatalog setupImageCatalogProvider(String catalogUrl, String catalogFile) throws IOException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        ImageCatalog catalog = objectMapper.readValue(catalogJson, ImageCatalog.class);
        when(imageCatalogProvider.getImageCatalog(catalogUrl)).thenReturn(catalog);
        when(imageCatalogProvider.getImageCatalog(DEFAULT_CATALOG_URL)).thenReturn(catalog);
        return catalog;
    }

    private ImageSettingsRequest setupImageSettingsRequest(String id, String catalog, String os) {
        ImageSettingsRequest is = new ImageSettingsRequest();
        is.setId(id);
        is.setCatalog(catalog);
        is.setOs(os);
        return is;
    }
}