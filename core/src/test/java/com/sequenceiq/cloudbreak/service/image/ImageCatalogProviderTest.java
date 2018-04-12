package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RestClientUtil.class)
public class ImageCatalogProviderTest {

    private static final String CB_IMAGE_CATALOG_V2_JSON = "cb-image-catalog-v2.json";

    private static final String CB_IMAGE_CATALOG_NULL_FIELD_JSON = "cb-image-catalog-null-field.json";

    private static final String CB_IMAGE_CATALOG_VALID_JSON = "cb-image-catalog-valid.json";

    private static final String CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON = "cb-image-catalog-filter-null-images.json";

    private static final String CB_IMAGE_CATALOG_EMPTY_CLOUDBREAK_VERSIONS_JSON = "cb-image-catalog-empty-cloudbreak-versions.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES = "cb-image-catalog-without-base-images.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_HDP_IMAGES = "cb-image-catalog-without-hdp-images.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_HDF_IMAGES = "cb-image-catalog-without-hdf-images.json";

    private static final String CB_VERSION = "1.16.4";

    private static final List<String> CB_IMAGE_CATALOG_V2_OS_TYPES = Lists.newArrayList("amazonlinux", "centos7");

    private static final List<String> CB_AMAZONLINUX_FILTER = Lists.newArrayList("amazonlinux");

    @InjectMocks
    private CachedImageCatalogProvider underTest;

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

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testReadImageCatalogFromFile() throws Exception {

        String path = getPath(CB_IMAGE_CATALOG_V2_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        CloudbreakImageCatalogV2 catalog = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_V2_JSON);

        assertNotNull("Check that the parsed ImageCatalog not null.", catalog);
        Optional<CloudbreakVersion> ver = catalog.getVersions().getCloudbreakVersions().stream().filter(v -> v.getVersions().contains(CB_VERSION)).findFirst();
        Assert.assertTrue("Check that the parsed ImageCatalog contains the desired version of Cloudbreak.", ver.isPresent());
        List<String> imageIds = ver.get().getImageIds();
        assertNotNull("Check that the parsed ImageCatalog contains the desired version of Cloudbreak with image id(s).", imageIds);
        Optional<String> imageIdOptional = imageIds.stream().findFirst();
        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image reference for the Cloudbreak version.", imageIdOptional.isPresent());
        String imageId = imageIdOptional.get();
        boolean baseImageFound = false;
        boolean hdpImageFound = false;
        boolean hdfImageFoiund = false;
        if (catalog.getImages().getBaseImages() != null) {
            baseImageFound = catalog.getImages().getBaseImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        if (catalog.getImages().getHdpImages() != null) {
            hdpImageFound = catalog.getImages().getHdpImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        if (catalog.getImages().getHdfImages() != null) {
            hdfImageFoiund = catalog.getImages().getHdfImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        boolean anyImageFoundForVersion = baseImageFound || hdpImageFound || hdfImageFoiund;
        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image for the Cloudbreak version.", anyImageFoundForVersion);
    }

    @Test
    public void testImageCatalogErrorWhenNull() throws CloudbreakImageCatalogException {
        String path = getPath(CB_IMAGE_CATALOG_NULL_FIELD_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        String errorMessage = getErrorMessage(CB_IMAGE_CATALOG_NULL_FIELD_JSON);

        String expected = "Missing required creator property 'images' (index 0)";
        Assert.assertTrue("Check that the 'images' field is missing", errorMessage.startsWith(expected));

    }

    private String getPath(String catalogUrl) {
        try {
            return TestUtil.getFilePath(getClass(), catalogUrl).getParent().toString();
        } catch (Exception e) {

            throw new RuntimeException(String.format("Catalog JSON cannot be read: %s", catalogUrl), e);
        }
    }

    @Test
    public void testImageCatalogValid() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_VALID_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        underTest.getImageCatalogV2(CB_IMAGE_CATALOG_VALID_JSON);
    }

    @Test
    public void testImageCatalogFilterNothing() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_V2_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", Collections.emptyList());

        CloudbreakImageCatalogV2 actualCatalog = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_V2_JSON);

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(CB_IMAGE_CATALOG_V2_OS_TYPES, actualOsTypes);

        ObjectMapper objectMapper = new ObjectMapper();
        CloudbreakImageCatalogV2 expectedCatalog = objectMapper.readValue(Paths.get(path, CB_IMAGE_CATALOG_V2_JSON).toFile(), CloudbreakImageCatalogV2.class);

        assertEquals(mapToUuid(expectedCatalog.getImages().getBaseImages()), mapToUuid(actualCatalog.getImages().getBaseImages()));
        assertEquals(mapToUuid(expectedCatalog.getImages().getHdpImages()), mapToUuid(actualCatalog.getImages().getHdpImages()));
        assertEquals(mapToUuid(expectedCatalog.getImages().getHdfImages()), mapToUuid(actualCatalog.getImages().getHdfImages()));
    }

    private List<String> mapToUuid(List<Image> imageList) {
        return imageList.stream()
                .map(Image::getUuid)
                .collect(Collectors.toList());
    }

    @Test
    public void testImageCatalogFilterToAmazonlinux() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_V2_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);
        ReflectionTestUtils.setField(underTest, "enabledLinuxTypes", CB_AMAZONLINUX_FILTER);

        CloudbreakImageCatalogV2 actualCatalog = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_V2_JSON);

        List<String> actualOsTypes = getImageCatalogOses(actualCatalog);
        assertEquals(CB_AMAZONLINUX_FILTER, actualOsTypes);

        ObjectMapper objectMapper = new ObjectMapper();
        CloudbreakImageCatalogV2 expectedCatalog = objectMapper.readValue(Paths.get(path, CB_IMAGE_CATALOG_V2_JSON).toFile(), CloudbreakImageCatalogV2.class);

        assertEquals(mapToUuid(expectedCatalog.getImages().getBaseImages()), mapToUuid(actualCatalog.getImages().getBaseImages()));
        List<String> expectedHdpImagesList = Collections.singletonList("2.4.0.0-1157-6eff4b14-482f-4e31-8e15-cb3a153d1030-2.5.0.0-1200");
        assertEquals(expectedHdpImagesList, mapToUuid(actualCatalog.getImages().getHdpImages()));
        assertEquals(mapToUuid(expectedCatalog.getImages().getHdfImages()), mapToUuid(actualCatalog.getImages().getHdfImages()));
    }

    private List<String> getImageCatalogOses(CloudbreakImageCatalogV2 actualCatalog) {
        return Stream.concat(actualCatalog.getImages().getBaseImages().stream(),
                Stream.concat(actualCatalog.getImages().getHdfImages().stream(),
                        actualCatalog.getImages().getHdpImages().stream()))
                .map(Image::getOs)
                .distinct()
                .collect(Collectors.toList());
    }

    @Test
    public void testHttpImageCatalogValid() throws CloudbreakImageCatalogException, IOException {
        PowerMockito.mockStatic(RestClientUtil.class);

        String path = getPath(CB_IMAGE_CATALOG_VALID_JSON);
        String catalogUrl = "http";

        when(RestClientUtil.get()).thenReturn(clientMock);
        when(clientMock.target(catalogUrl)).thenReturn(webTargetMock);
        when(webTargetMock.request()).thenReturn(builderMock);
        when(builderMock.get()).thenReturn(responseMock);
        when(responseMock.getStatusInfo()).thenReturn(statusTypeMock);
        when(statusTypeMock.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
        when(responseMock.readEntity(String.class)).thenReturn(FileUtils.readFileToString(Paths.get(path, CB_IMAGE_CATALOG_V2_JSON).toFile()));

        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        CloudbreakImageCatalogV2 actualCatalog = underTest.getImageCatalogV2(catalogUrl);

        CloudbreakImageCatalogV2 expectedCatalog = objectMapper.readValue(Paths.get(path, CB_IMAGE_CATALOG_V2_JSON).toFile(), CloudbreakImageCatalogV2.class);

        assertEquals(mapToUuid(expectedCatalog.getImages().getBaseImages()), mapToUuid(actualCatalog.getImages().getBaseImages()));
        assertEquals(mapToUuid(expectedCatalog.getImages().getHdpImages()), mapToUuid(actualCatalog.getImages().getHdpImages()));
        assertEquals(mapToUuid(expectedCatalog.getImages().getHdfImages()), mapToUuid(actualCatalog.getImages().getHdfImages()));
    }

    @Test(expected = CloudbreakImageCatalogException.class)
    public void testHttpImageCatalogNotValidJson() throws CloudbreakImageCatalogException, IOException {
        PowerMockito.mockStatic(RestClientUtil.class);

        String path = getPath(CB_IMAGE_CATALOG_VALID_JSON);
        String catalogUrl = "http";

        when(RestClientUtil.get()).thenReturn(clientMock);
        when(clientMock.target(catalogUrl)).thenReturn(webTargetMock);
        when(webTargetMock.request()).thenReturn(builderMock);
        when(builderMock.get()).thenReturn(responseMock);
        when(responseMock.getStatusInfo()).thenReturn(statusTypeMock);
        when(statusTypeMock.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
        when(responseMock.readEntity(String.class)).thenReturn("image catalog");

        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        underTest.getImageCatalogV2(catalogUrl);
    }

    @Test
    public void testImageCatalogWithEmptyCloudBreakVersions() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_EMPTY_CLOUDBREAK_VERSIONS_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        String errorMessage = getErrorMessage(CB_IMAGE_CATALOG_EMPTY_CLOUDBREAK_VERSIONS_JSON);
        String expected = "Cloudbreak versions cannot be NULL";
        Assert.assertTrue("Check that the Cloudbreak version cannot be empty", errorMessage.startsWith(expected));
    }

    @Test
    public void testImageCatalogFilterNullImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON);
        assertEquals(1, imageCatalogV2.getImages().getBaseImages().get(0).getImageSetsByProvider().values().size());
    }

    @Test
    public void testImageCatalogWithoutHdfImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_HDF_IMAGES);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_WITHOUT_HDF_IMAGES);
        assertNotNull(imageCatalogV2.getImages().getHdfImages());
    }

    @Test
    public void testImageCatalogWithoutHdpImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_HDP_IMAGES);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_WITHOUT_HDP_IMAGES);
        assertNotNull(imageCatalogV2.getImages().getHdpImages());
    }

    @Test
    public void testImageCatalogWithoutBaseImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES);
        ReflectionTestUtils.setField(underTest, "etcConfigDir", path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES);
        assertNotNull(imageCatalogV2.getImages().getBaseImages());
    }

    private String getErrorMessage(String catalogUrl) {
        String errorMessage = "";
        try {
            underTest.getImageCatalogV2(catalogUrl);
        } catch (CloudbreakImageCatalogException e) {
            errorMessage = e.getMessage();
        }

        return errorMessage;
    }

//    @Test
//    public void testReadImageCatalogFromHTTP() {
//        CloudbreakImageCatalogV2 catalog = underTest.getImageCatalogV2();
//
//        Assert.assertNotNull("Check that the parsed ImageCatalog not null.", catalog);
//        Optional<CloudbreakVersion> ver = catalog.getVersions().getCloudbreakVersions().stream().filter(v -> v.getVersions().contains(CB_VERSION)).findFirst();
//        Assert.assertTrue("Check that the parsed ImageCatalog contains the desired version of Cloudbreak.", ver.isPresent());
//        List<String> imageIds = ver.get().getImageIds();
//        Assert.assertNotNull("Check that the parsed ImageCatalog contains the desired version of Cloudbreak with image id(s).", imageIds);
//        Optional<String> imageIdOptional = ver.get().getImageIds().stream().findFirst();
//        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image reference for the Cloudbreak version.", imageIdOptional.isPresent());
//        String imageId = imageIdOptional.get();
//        boolean baseImageFound = false;
//        boolean hdpImageFound = false;
//        boolean hdfImageFoiund = false;
//        if (catalog.getImages().getBaseImages() != null) {
//            baseImageFound = catalog.getImages().getBaseImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        if (catalog.getImages().getHdpImages() != null) {
//            hdpImageFound = catalog.getImages().getHdpImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        if (catalog.getImages().getHdfImages() != null) {
//            hdfImageFoiund = catalog.getImages().getHdfImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        boolean anyImageFoundForVersion = baseImageFound || hdpImageFound || hdfImageFoiund;
//        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image for the Cloudbreak version.", anyImageFoundForVersion);
//    }
}