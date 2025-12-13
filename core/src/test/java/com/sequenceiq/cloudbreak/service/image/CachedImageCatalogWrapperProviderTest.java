package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;

@ExtendWith(MockitoExtension.class)
public class CachedImageCatalogWrapperProviderTest {

    private static final String CB_IMAGE_CATALOG_V2_JSON = "cb-image-catalog-v2.json";

    private static final String FREEIPA_IMAGE_CATALOG_V3_JSON = "freeipa-image-catalog-v3.json";

    private static final String CB_IMAGE_CATALOG_RC_JSON = "cb-rc-image-catalog.json";

    private static final String CB_IMAGE_CATALOG_NULL_FIELD_JSON = "cb-image-catalog-null-field.json";

    private static final String CB_IMAGE_CATALOG_VALID_JSON = "cb-image-catalog-valid.json";

    private static final String CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON = "cb-image-catalog-filter-null-images.json";

    private static final String CB_IMAGE_CATALOG_FILTER_IMAGES_NULL_JSON = "cb-image-catalog-filter-images-null.json";

    private static final String FREEIPA_IMAGE_CATALOG_FILTER_IMAGES_NULL_JSON = "freeipa-image-catalog-filter-images-null.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES = "cb-image-catalog-without-base-images.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_CDH_IMAGES = "cb-image-catalog-without-cdh-images.json";

    private static final String CB_VERSION = "1.16.5";

    private static final List<String> RC_IMAGE_CATALOG_OS_TYPES = Lists.newArrayList("amazonlinux", "centos7", "amazonlinux2", "sles12", "ubuntu16");

    private static final List<String> CB_AMAZONLINUX_FILTER = Lists.newArrayList("amazonlinux");

    private static final List<String> CB_REDHAT7_FILTER = Lists.newArrayList("redhat7");

    @InjectMocks
    private CachedImageCatalogWrapperProvider underTest;

    @Mock
    private Client clientMock;

    @Mock
    private WebTarget webTargetMock;

    @Mock
    private Invocation.Builder builderMock;

    @Mock
    private Response responseMock;

    @Mock
    private Response.StatusType statusTypeMock;

    @Mock
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Mock
    private RestClientFactory restClientFactory;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testReadImageCatalogFromFile() throws Exception {

        String path = getPath(CB_IMAGE_CATALOG_V2_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        CloudbreakImageCatalogV3 catalog = underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_V2_JSON).getImageCatalog();

        assertNotNull(catalog);
        Optional<CloudbreakVersion> ver = catalog.getVersions().getCloudbreakVersions().stream().filter(v -> v.getVersions().contains(CB_VERSION)).findFirst();
        assertTrue(ver.isPresent(), "Check that the parsed ImageCatalog contains the desired version of Cloudbreak.");
        List<String> imageIds = ver.get().getImageIds();
        assertNotNull(imageIds);
        Optional<String> imageIdOptional = imageIds.stream().findFirst();
        assertTrue(imageIdOptional.isPresent(), "Check that the parsed ImageCatalog contains image reference for the Cloudbreak version.");
        String imageId = imageIdOptional.get();
        boolean baseImageFound = false;
        if (catalog.getImages().getBaseImages() != null) {
            baseImageFound = catalog.getImages().getBaseImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        assertTrue(baseImageFound, "Check that the parsed ImageCatalog contains image for the Cloudbreak version.");
    }

    @Test
    public void testImageCatalogErrorWhenNull() {
        String path = getPath(CB_IMAGE_CATALOG_NULL_FIELD_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        String errorMessage = getErrorMessage(CB_IMAGE_CATALOG_NULL_FIELD_JSON);

        String expected = "Missing required creator property 'images' (index 0)";
        assertTrue(errorMessage.startsWith(expected), "Check that the 'images' field is missing");

    }

    private String getPath(String catalogUrl) {
        try {
            return TestUtil.getFilePath(getClass(), catalogUrl).getParent().toString();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Catalog JSON cannot be read: %s", catalogUrl), e);
        }
    }

    @Test
    public void testImageCatalogValid() throws CloudbreakImageCatalogException {
        String path = getPath(CB_IMAGE_CATALOG_VALID_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());
        underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_VALID_JSON);
    }

    @Test
    public void testImageCatalogFilterNothing() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_RC_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        CloudbreakImageCatalogV3 actualCatalog = underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_RC_JSON).getImageCatalog();

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(RC_IMAGE_CATALOG_OS_TYPES, actualOsTypes);

        ObjectMapper objectMapper = new ObjectMapper();
        CloudbreakImageCatalogV3 expectedCatalog =
                objectMapper.readValue(Paths.get(path, CB_IMAGE_CATALOG_RC_JSON).toFile(), CloudbreakImageCatalogV3.class);

        assertEquals(mapToUuid(expectedCatalog.getImages().getBaseImages()), mapToUuid(actualCatalog.getImages().getBaseImages()));
        assertEquals(mapToUuid(expectedCatalog.getImages().getCdhImages()), mapToUuid(actualCatalog.getImages().getCdhImages()));
    }

    private List<String> mapToUuid(List<Image> imageList) {
        return imageList.stream()
                .map(Image::getUuid)
                .collect(Collectors.toList());
    }

    @Test
    public void testCbImageCatalogFilterToAmazonlinux() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_V2_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", CB_AMAZONLINUX_FILTER);

        CloudbreakImageCatalogV3 actualCatalog = underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_V2_JSON).getImageCatalog();

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(CB_AMAZONLINUX_FILTER, actualOsTypes);

        ObjectMapper objectMapper = new ObjectMapper();
        CloudbreakImageCatalogV3 expectedCatalog = objectMapper.readValue(Paths.get(path, CB_IMAGE_CATALOG_V2_JSON).toFile(), CloudbreakImageCatalogV3.class);

        assertEquals(mapToUuid(expectedCatalog.getImages().getBaseImages()), mapToUuid(actualCatalog.getImages().getBaseImages()));
        assertEquals(mapToUuid(Collections.emptyList()), mapToUuid(actualCatalog.getImages().getCdhImages()));
        assertEquals(3, expectedCatalog.getImages().getCdhImages().size());
    }

    @Test
    public void testFreeipaImageCatalogFilterToRedhat7() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(FREEIPA_IMAGE_CATALOG_V3_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", CB_REDHAT7_FILTER);

        CloudbreakImageCatalogV3 actualCatalog = underTest.getImageCatalogWrapper(FREEIPA_IMAGE_CATALOG_V3_JSON).getImageCatalog();

        assertEquals(mapToUuid(Collections.emptyList()), mapToUuid(actualCatalog.getImages().getBaseImages()));
        assertEquals(mapToUuid(Collections.emptyList()), mapToUuid(actualCatalog.getImages().getCdhImages()));
        assertEquals(1, actualCatalog.getImages().getFreeIpaImages().size());
        assertEquals("81851893-8340-411d-afb7-e1b55107fb10", actualCatalog.getImages().getFreeIpaImages().get(0).getUuid());
    }

    private List<String> getImageCatalogOses(CloudbreakImageCatalogV3 actualCatalog) {
        return actualCatalog.getImages().getBaseImages().stream()
                .map(Image::getOs)
                .distinct()
                .collect(Collectors.toList());
    }

    @Test
    public void testHttpImageCatalogValid() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_VALID_JSON);
        String catalogUrl = "http";

        when(restClientFactory.getOrCreateWithFollowRedirects()).thenReturn(clientMock);
        when(clientMock.target(catalogUrl)).thenReturn(webTargetMock);
        when(webTargetMock.request()).thenReturn(builderMock);
        when(builderMock.get()).thenReturn(responseMock);
        when(responseMock.getStatusInfo()).thenReturn(statusTypeMock);
        when(statusTypeMock.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
        when(responseMock.readEntity(String.class)).thenReturn(Files.readString(Paths.get(path, CB_IMAGE_CATALOG_V2_JSON)));

        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        CloudbreakImageCatalogV3 actualCatalog = underTest.getImageCatalogWrapper(catalogUrl).getImageCatalog();

        CloudbreakImageCatalogV3 expectedCatalog = objectMapper.readValue(Paths.get(path, CB_IMAGE_CATALOG_V2_JSON).toFile(), CloudbreakImageCatalogV3.class);

        assertEquals(mapToUuid(expectedCatalog.getImages().getBaseImages()), mapToUuid(actualCatalog.getImages().getBaseImages()));
        assertEquals(mapToUuid(expectedCatalog.getImages().getCdhImages()), mapToUuid(actualCatalog.getImages().getCdhImages()));

    }

    @Test
    public void testHttpImageCatalogNotValidJson() {
        String path = getPath(CB_IMAGE_CATALOG_VALID_JSON);
        String catalogUrl = "http";

        when(restClientFactory.getOrCreateWithFollowRedirects()).thenReturn(clientMock);
        when(clientMock.target(catalogUrl)).thenReturn(webTargetMock);
        when(webTargetMock.request()).thenReturn(builderMock);
        when(builderMock.get()).thenReturn(responseMock);
        when(responseMock.getStatusInfo()).thenReturn(statusTypeMock);
        when(statusTypeMock.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
        when(responseMock.readEntity(String.class)).thenReturn("image catalog");

        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        assertThrows(CloudbreakImageCatalogException.class, () -> underTest.getImageCatalogWrapper(catalogUrl));
    }

    @Test
    public void testImageCatalogFilterNullImages() throws CloudbreakImageCatalogException {
        String path = getPath(CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        CloudbreakImageCatalogV3 imageCatalogV2 = underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON).getImageCatalog();
        assertEquals(1L, imageCatalogV2.getImages().getBaseImages().get(0).getImageSetsByProvider().values().size());
    }

    @Test
    public void testCbImageCatalogFilterImagesNull() throws CloudbreakImageCatalogException {
        String path = getPath(CB_IMAGE_CATALOG_FILTER_IMAGES_NULL_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        assertThrows(CloudbreakImageCatalogException.class, () -> underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_FILTER_IMAGES_NULL_JSON));
    }

    @Test
    public void testFreeipaImageCatalogFilterImagesNull() throws CloudbreakImageCatalogException {
        String path = getPath(FREEIPA_IMAGE_CATALOG_FILTER_IMAGES_NULL_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        assertThrows(CloudbreakImageCatalogException.class, () -> underTest.getImageCatalogWrapper(FREEIPA_IMAGE_CATALOG_FILTER_IMAGES_NULL_JSON));
    }

    @Test
    public void testImageCatalogWithoutBaseImages() throws CloudbreakImageCatalogException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        CloudbreakImageCatalogV3 imageCatalogV2 = underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES).getImageCatalog();
        assertNotNull(imageCatalogV2.getImages().getBaseImages());
    }

    @Test
    public void testImageCatalogWithoutCdhImages() throws CloudbreakImageCatalogException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_CDH_IMAGES);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        CloudbreakImageCatalogV3 imageCatalogV2 = underTest.getImageCatalogWrapper(CB_IMAGE_CATALOG_WITHOUT_CDH_IMAGES).getImageCatalog();
        assertNotNull(imageCatalogV2.getImages().getCdhImages());
    }

    private String getErrorMessage(String catalogUrl) {
        String errorMessage = "";
        try {
            underTest.getImageCatalogWrapper(catalogUrl);
        } catch (CloudbreakImageCatalogException e) {
            errorMessage = e.getMessage();
        }

        return errorMessage;
    }
}
