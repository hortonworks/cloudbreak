package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;

@ExtendWith(MockitoExtension.class)
class ImageToImageV4ResponseConverterTest extends AbstractEntityConverterTest<Image> {

    private static final String TEST_DATE = "2021.04.26.";

    private static final Long TEST_CREATED = 123456L;

    private static final Long TEST_PUBLISHED = 234567L;

    private static final String TEST_DESCRIPTION = "Official test image";

    private static final String TEST_OS = "redhat7";

    private static final String TEST_OS_TYPE = "centos";

    private static final String TEST_UUID = "d13b2af6-78d8-4ebf-6058-0f74d1e14906";

    private static final String TEST_VERSION = "7.2.2";

    private static final String TEST_CM_BUILD_NUMBER = "4227375";

    private static final String TEST_BASE_PARCEL_URL = "http://myBaseUrl/";

    private static final String TEST_SOURCE_IMAGE_ID = "fb4f771d-4712-4e15-75c2-bd12eb525d2a";

    private static final String TEST_PLATFORM = "aws";

    private static final String ARCHITECTURE = "arm64";

    @InjectMocks
    private ImageToImageV4ResponseConverter underTest;

    @Override
    public Image createSource() {
        return getTestImage(ARCHITECTURE);
    }

    @Test
    void testConvert() {

        ImageV4Response result = underTest.convert(createSource());

        validateImageV4Response(result, false, ARCHITECTURE);
    }

    @Test
    void testConvertWithoutCollections() {

        ImageV4Response result = underTest.convert(getTestImageWithoutCollections(ARCHITECTURE));

        validateImageV4Response(result, true, ARCHITECTURE);
    }

    @Test
    void testNullArchitecture() {
        ImageV4Response result = underTest.convert(getTestImageWithoutCollections(null));

        validateImageV4Response(result, true, "x86_64");
    }

    @Test
    void testUnknownArchitecture() {
        ImageV4Response result = underTest.convert(getTestImageWithoutCollections("aarch64"));

        validateImageV4Response(result, true, "unknown");
    }

    private static Image getTestImage(String architecture) {

        Map<String, Map<String, String>> imageSetsByProvider =
                Collections.singletonMap(TEST_PLATFORM, Collections.singletonMap("eu-west-1", "ami-03f39b6f019026862"));
        Map<String, String> packageVersions = Collections.singletonMap("cdh-build-number", "4217674");

        Map<String, String> repo = Collections.singletonMap("redhat7", "http://cloudera-build-us-west-1.vpc.cloudera.com/");

        Map<String, String> stackDetailsMap = Collections.singletonMap("repoid", "CDH-7.2.1");
        StackRepoDetails repoDetails = new StackRepoDetails(stackDetailsMap, Collections.emptyMap());
        ImageStackDetails stackDetails = new ImageStackDetails("7.2.1", repoDetails, "7.2.1-1.cdh7.2.1.p0.4217674");

        List<List<String>> preWarmParcels = List.of(List.of("PROFILER-2.0.3.2.0.3.0", "http://s3.amazonaws.com/dev.hortonworks.com/DSS/centos7/"));
        List<String> preWarmCsd = List.of("http://s3.amazonaws.com/dev.hortonworks.com/DSS/PROFILER-2.0.3.jar");

        return Image.builder()
                .copy(getTestImageWithoutCollections(architecture))
                .withRepo(repo)
                .withImageSetsByProvider(imageSetsByProvider)
                .withStackDetails(stackDetails)
                .withPackageVersions(packageVersions)
                .withPreWarmParcels(preWarmParcels)
                .withPreWarmCsd(preWarmCsd)
                .withArchitecture(architecture)
                .build();
    }

    private static Image getTestImageWithoutCollections(String architecture) {
        return Image.builder()
                .withDate(TEST_DATE)
                .withCreated(TEST_CREATED)
                .withPublished(TEST_PUBLISHED)
                .withDescription(TEST_DESCRIPTION)
                .withOs(TEST_OS)
                .withArchitecture(architecture)
                .withUuid(TEST_UUID)
                .withVersion(TEST_VERSION)
                .withOsType(TEST_OS_TYPE)
                .withCmBuildNumber(TEST_CM_BUILD_NUMBER)
                .withBaseParcelUrl(TEST_BASE_PARCEL_URL)
                .withSourceImageId(TEST_SOURCE_IMAGE_ID)
                .withDefaultImage(true)
                .build();
    }

    private void validateImageV4Response(ImageV4Response response, boolean emptyCollections, String architecture) {
        assertEquals(TEST_DATE, response.getDate());
        assertEquals(TEST_CREATED, response.getCreated());
        assertEquals(TEST_PUBLISHED, response.getPublished());
        assertEquals(TEST_DESCRIPTION, response.getDescription());
        assertEquals(TEST_OS, response.getOs());
        assertEquals(TEST_OS_TYPE, response.getOsType());
        assertEquals(architecture, response.getArchitecture());
        assertEquals(TEST_UUID, response.getUuid());
        assertEquals(TEST_VERSION, response.getVersion());
        assertTrue(response.isDefaultImage());
        assertEquals(TEST_CM_BUILD_NUMBER, response.getCmBuildNumber());
        assertEquals(TEST_BASE_PARCEL_URL, response.getBaseParcelUrl());
        assertEquals(TEST_SOURCE_IMAGE_ID, response.getSourceImageId());

        if (emptyCollections) {
            assertTrue(response.getPackageVersions().isEmpty());
            assertTrue(response.getRepository().isEmpty());
            assertNull(response.getImageSetsByProvider());
            assertTrue(response.getPreWarmParcels().isEmpty());
            assertTrue(response.getPreWarmCsd().isEmpty());
            assertNull(response.getStackDetails());
        } else {
            assertFalse(response.getPackageVersions().isEmpty());
            assertFalse(response.getRepository().isEmpty());
            assertFalse(response.getImageSetsByProvider().isEmpty());
            assertFalse(response.getPreWarmParcels().isEmpty());
            assertFalse(response.getPreWarmCsd().isEmpty());
            assertNotNull(response.getStackDetails());
        }
    }
}