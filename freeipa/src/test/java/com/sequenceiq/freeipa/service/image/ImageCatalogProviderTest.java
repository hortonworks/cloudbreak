package com.sequenceiq.freeipa.service.image;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.freeipa.TestUtil;
import com.sequenceiq.freeipa.api.model.image.FreeIpaVersions;
import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.model.image.ImageCatalog;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogProviderTest {

    private static final String IMAGE_CATALOG_JSON = "freeipa-catalog-1.json";

    private static final String IMAGE_CATALOG_INVALID_FIELD_JSON = "freeipa-catalog-invalid-field.json";

    private static final String IMAGE_CATALOG_NULL_FIELD_JSON = "freeipa-catalog-null-field.json";

    private static final String IMAGE_CATALOG_INVALID_JSON = "freeipa-catalog-invalid-json.json";

    private static final String IMAGE_CATALOG_FILTER_ALL_OS_JSON = "freeipa-catalog-filter-all-os.json";

    private static final List<String> IMAGE_CATALOG_OS_TYPES = Lists.newArrayList("redhat7", "centos7");

    private static final List<String> CB_CENTOS_7_FILTER = Lists.newArrayList("centos7");

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private ImageCatalogProvider underTest;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testReadImageCatalogFromFile() {

        String path = getPath(IMAGE_CATALOG_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        ImageCatalog catalog = underTest.getImageCatalog(IMAGE_CATALOG_JSON);
        List<com.sequenceiq.freeipa.api.model.image.Image> images = catalog.getImages().getFreeipaImages();
        assertEquals(4, images.size());
        assertEquals("61851893-8340-411d-afb7-e1b55107fb10", images.get(0).getUuid());

        FreeIpaVersions freeIpaVersions = catalog.getVersions().getFreeIpaVersions().get(0);
        assertEquals(2, freeIpaVersions.getImageIds().size());
        assertEquals("61851893-8340-411d-afb7-e1b55107fb10", freeIpaVersions.getImageIds().get(0));
        assertEquals(1, freeIpaVersions.getDefaults().size());
        assertEquals(List.of("71851893-8340-411d-afb7-e1b55107fb10"), freeIpaVersions.getDefaults());
        assertEquals(4, freeIpaVersions.getVersions().size());
    }

    @Test
    public void testImageCatalogErrorWhenNull() {
        String path = getPath(IMAGE_CATALOG_NULL_FIELD_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        String errorMessage = getErrorMessage(IMAGE_CATALOG_NULL_FIELD_JSON);

        String expected = "Invalid json format for image catalog with error: Missing required creator property 'images'";
        Assert.assertTrue("Check that the 'images' field is missing", errorMessage.startsWith(expected));
    }

    @Test
    public void testImageCatalogErrorWhenInvalid() {
        String path = getPath(IMAGE_CATALOG_INVALID_FIELD_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        String errorMessage = getErrorMessage(IMAGE_CATALOG_INVALID_FIELD_JSON);

        String expected = "Invalid json format for image catalog with error: Missing required creator property 'images'";
        Assert.assertTrue("Check that the 'images' field is missing", errorMessage.startsWith(expected));
    }

    @Test
    public void testImageCatalogErrorWhenInvalidJson() {
        String path = getPath(IMAGE_CATALOG_INVALID_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        String errorMessage = getErrorMessage(IMAGE_CATALOG_INVALID_JSON);

        String expected = "Failed to read image catalog from file";
        Assert.assertTrue("Check that the json is invalid", errorMessage.startsWith(expected));
    }

    @Test
    public void testImageCatalogFilterNothing() {
        String path = getPath(IMAGE_CATALOG_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        ImageCatalog actualCatalog = underTest.getImageCatalog(IMAGE_CATALOG_JSON);

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(IMAGE_CATALOG_OS_TYPES, actualOsTypes);

        List<String> expectedIds = List.of("61851893-8340-411d-afb7-e1b55107fb10", "71851893-8340-411d-afb7-e1b55107fb10",
                "81851893-8340-411d-afb7-e1b55107fb10", "91851893-8340-411d-afb7-e1b55107fb10");
        assertEquals(expectedIds, mapToUuid(actualCatalog.getImages().getFreeipaImages()));
        assertEquals(List.of("61851893-8340-411d-afb7-e1b55107fb10", "71851893-8340-411d-afb7-e1b55107fb10"),
                actualCatalog.getVersions().getFreeIpaVersions().get(0).getImageIds());
    }

    @Test
    public void testImageCatalogFilterOs() {
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
    public void testImageCatalogFilterAllOs() {
        String path = getPath(IMAGE_CATALOG_FILTER_ALL_OS_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", CB_CENTOS_7_FILTER);

        ImageCatalog actualCatalog = underTest.getImageCatalog(IMAGE_CATALOG_FILTER_ALL_OS_JSON);

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(Collections.emptyList(), actualOsTypes);
        assertEquals(Collections.emptyList(), mapToUuid(actualCatalog.getImages().getFreeipaImages()));
        assertEquals(0, actualCatalog.getImages().getFreeipaImages().size());
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