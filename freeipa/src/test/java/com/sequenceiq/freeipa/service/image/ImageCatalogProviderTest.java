package com.sequenceiq.freeipa.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.freeipa.TestUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.FreeIpaVersions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;

@ExtendWith(MockitoExtension.class)
class ImageCatalogProviderTest {

    private static final String IMAGE_CATALOG_JSON = "freeipa-catalog-1.json";

    private static final String IMAGE_CATALOG_WITHOUT_VERSIONS = "freeipa-catalog-without-versions-but-with-advertised-flag.json";

    private static final String NULL_IMAGE_CATALOG = "null-freeipa-catalog.json";

    private static final String SECRET_FILE = "file_with_secret.whatever";

    private static final String IMAGE_CATALOG_INVALID_FIELD_JSON = "freeipa-catalog-invalid-field.json";

    private static final String IMAGE_CATALOG_NULL_FIELD_JSON = "freeipa-catalog-null-field.json";

    private static final String IMAGE_CATALOG_INVALID_JSON = "freeipa-catalog-invalid-json.json";

    private static final String IMAGE_CATALOG_FILTER_ALL_OS_JSON = "freeipa-catalog-filter-all-os.json";

    private static final List<String> IMAGE_CATALOG_OS_TYPES = Lists.newArrayList("redhat7", "centos7", "redhat8");

    private static final List<String> CB_CENTOS_7_FILTER = Lists.newArrayList("centos7");

    private static final String DEFAULT_VERSION = "2.20.0-dev.1";

    @InjectMocks
    private ImageCatalogProvider underTest;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void initTests() {
        ReflectionTestUtils.setField(underTest, ImageCatalogProvider.class, "freeIpaVersion", DEFAULT_VERSION, null);
    }

    @Test
    void testReadImageCatalogFromFile() {

        String path = getPath(IMAGE_CATALOG_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        ImageCatalog catalog = underTest.getImageCatalog(IMAGE_CATALOG_JSON);
        List<com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image> images = catalog.getImages().getFreeipaImages();
        assertEquals(6, images.size());
        assertEquals("61851893-8340-411d-afb7-e1b55107fb10", images.get(0).getUuid());

        FreeIpaVersions freeIpaVersions = catalog.getVersions().getFreeIpaVersions().get(0);
        assertEquals(3, freeIpaVersions.getImageIds().size());
        assertEquals("61851893-8340-411d-afb7-e1b55107fb10", freeIpaVersions.getImageIds().get(0));
        assertEquals(2, freeIpaVersions.getDefaults().size());
        assertEquals(List.of("71851893-8340-411d-afb7-e1b55107fb10", "b465c893-fe04-44b1-ae8e-0452bbb39c99"), freeIpaVersions.getDefaults());
        assertEquals(4, freeIpaVersions.getVersions().size());
    }

    @Test
    void testReadImageCatalogFromFileWithoutVersionsButWithAdvertisedFlag() {

        String path = getPath(IMAGE_CATALOG_WITHOUT_VERSIONS);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", CB_CENTOS_7_FILTER);

        ImageCatalog catalog = underTest.getImageCatalog(IMAGE_CATALOG_WITHOUT_VERSIONS);
        List<com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image> images = catalog.getImages().getFreeipaImages();
        assertEquals(2, images.size());

        FreeIpaVersions freeIpaVersions = catalog.getVersions().getFreeIpaVersions().get(0);
        assertEquals(1, freeIpaVersions.getImageIds().size());
        assertEquals("91851893-8340-411d-afb7-e1b55107fb10", freeIpaVersions.getImageIds().get(0));
    }

    @Test
    void testImageCatalogErrorWhenNull() {
        String path = getPath(IMAGE_CATALOG_NULL_FIELD_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        String errorMessage = getErrorMessage(IMAGE_CATALOG_NULL_FIELD_JSON);

        String expected = "Invalid json format for image catalog with error: Missing required creator property 'images'";
        assertTrue(errorMessage.startsWith(expected), "Check that the 'images' field is missing");
    }

    @Test
    void testImageCatalogErrorWhenInvalid() {
        String path = getPath(IMAGE_CATALOG_INVALID_FIELD_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        String errorMessage = getErrorMessage(IMAGE_CATALOG_INVALID_FIELD_JSON);

        String expected = "Invalid json format for image catalog with error: Missing required creator property 'images'";
        assertTrue(errorMessage.startsWith(expected), "Check that the 'images' field is missing");
    }

    @Test
    void testImageCatalogErrorWhenInvalidJson() {
        String path = getPath(IMAGE_CATALOG_INVALID_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        String errorMessage = getErrorMessage(IMAGE_CATALOG_INVALID_JSON);

        String expected = "Failed to read image catalog from file";
        assertTrue(errorMessage.startsWith(expected), "Check that the json is invalid");
    }

    @Test
    void testImageCatalogFilterNothing() {
        String path = getPath(IMAGE_CATALOG_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        ImageCatalog actualCatalog = underTest.getImageCatalog(IMAGE_CATALOG_JSON);

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(IMAGE_CATALOG_OS_TYPES, actualOsTypes);

        List<String> expectedIds = List.of("61851893-8340-411d-afb7-e1b55107fb10", "71851893-8340-411d-afb7-e1b55107fb10",
                "81851893-8340-411d-afb7-e1b55107fb10", "91851893-8340-411d-afb7-e1b55107fb10", "b465c893-fe04-44b1-ae8e-0452bbb39c99",
                "b465c893-fe04-44b1-ae8e-0452bbb39c99");
        assertEquals(expectedIds, mapToUuid(actualCatalog.getImages().getFreeipaImages()));
        assertEquals(List.of("61851893-8340-411d-afb7-e1b55107fb10", "71851893-8340-411d-afb7-e1b55107fb10", "b465c893-fe04-44b1-ae8e-0452bbb39c99"),
                actualCatalog.getVersions().getFreeIpaVersions().get(0).getImageIds());
    }

    @Test
    void testImageCatalogFilterOs() {
        String path = getPath(IMAGE_CATALOG_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", CB_CENTOS_7_FILTER);

        ImageCatalog actualCatalog = underTest.getImageCatalog(IMAGE_CATALOG_JSON);

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(CB_CENTOS_7_FILTER, actualOsTypes);

        List<String> expectedImagesList = List.of("81851893-8340-411d-afb7-e1b55107fb10", "91851893-8340-411d-afb7-e1b55107fb10");
        assertEquals(expectedImagesList, mapToUuid(actualCatalog.getImages().getFreeipaImages()));
        assertEquals(List.of(), actualCatalog.getVersions().getFreeIpaVersions().get(0).getImageIds());
        assertEquals(List.of(), actualCatalog.getVersions().getFreeIpaVersions().get(0).getDefaults());
        assertEquals(2, actualCatalog.getImages().getFreeipaImages().size());
    }

    @Test
    void testImageCatalogFilterAllOs() {
        String path = getPath(IMAGE_CATALOG_FILTER_ALL_OS_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", CB_CENTOS_7_FILTER);

        ImageCatalog actualCatalog = underTest.getImageCatalog(IMAGE_CATALOG_FILTER_ALL_OS_JSON);

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(Collections.emptyList(), actualOsTypes);
        assertEquals(Collections.emptyList(), mapToUuid(actualCatalog.getImages().getFreeipaImages()));
        assertEquals(0, actualCatalog.getImages().getFreeipaImages().size());
    }

    @Test
    void shouldThrowImageCatalogExceptionInCaseOfNullCatalogUrl() {
        Exception ex = assertThrows(ImageCatalogException.class, () -> underTest.getImageCatalog(null));
        assertEquals("Unable to fetch image catalog. The catalogUrl is null.", ex.getMessage());
    }

    @Test
    void shouldThrowImageCatalogExceptionInCaseOfNullCatalog() {
        String path = getPath(NULL_IMAGE_CATALOG);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        Exception ex = assertThrows(ImageCatalogException.class, () -> underTest.getImageCatalog(NULL_IMAGE_CATALOG));
        assertEquals("Failed to read the content of 'null-freeipa-catalog.json' as an image catalog.", ex.getMessage());
    }

    @Test
    void shouldThrowImageCatalogExceptionWithoutHavingSecretContent() {
        String path = getPath(SECRET_FILE);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        Throwable ex = assertThrows(ImageCatalogException.class, () -> underTest.getImageCatalog(SECRET_FILE));
        noSecretInExcepionMessage(ex);
    }

    private void noSecretInExcepionMessage(Throwable ex) {
        if (ex.getCause() != null) {
            noSecretInExcepionMessage(ex.getCause());
        }
        assertFalse(ex.getMessage().contains("topsecret"));
        assertFalse(ex.toString().contains("topsecret"));
    }

    private String getErrorMessage(String catalogUrl) {
        String errorMessage = "";
        try {
            underTest.getImageCatalog(catalogUrl);
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
        }

        return errorMessage;
    }

    private String getPath(String catalogUrl) {
        try {
            return TestUtil.getFilePath(getClass(), catalogUrl).getParent().toString();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Catalog JSON cannot be read: %s", catalogUrl), e);
        }
    }

    private List<String> getImageCatalogOses(ImageCatalog actualCatalog) {
        return actualCatalog.getImages().getFreeipaImages().stream()
                .map(Image::getOs)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> mapToUuid(List<Image> imageList) {
        return imageList.stream()
                .map(Image::getUuid)
                .collect(Collectors.toList());
    }
}