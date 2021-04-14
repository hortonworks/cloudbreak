package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ImageToImageV4ResponseConverterTest extends AbstractEntityConverterTest<Image> {

    private static final String TEST_DATE = "2021.04.26.";

    private static final Long TEST_CREATED = 123456L;

    private static final String TEST_DESCRIPTION = "Official test image";

    private static final String TEST_OS = "redhat7";

    private static final String TEST_OS_TYPE = "centos";

    private static final String TEST_UUID = "d13b2af6-78d8-4ebf-6058-0f74d1e14906";

    private static final String TEST_VERSION = "7.2.2";

    private static final String TEST_CM_BUILD_NUMBER = "4227375";

    private static final String TEST_BASE_PARCEL_URL = "http://myBaseUrl/";

    private static final String TEST_SOURCE_IMAGE_ID = "fb4f771d-4712-4e15-75c2-bd12eb525d2a";

    private static final String TEST_PLATFORM = "aws";

    @InjectMocks
    private ImageToImageV4ResponseConverter underTest;

    @Override
    public Image createSource() {
        return getTestImage();
    }

    @Test
    public void testConvert() {

        ImageV4Response result = underTest.convert(createSource());

        validateImageV4Response(result, false);
    }

    @Test
    public void testConvertWithoutCollections() {

        ImageV4Response result = underTest.convert(getTestImageWithoutCollections());

        validateImageV4Response(result, true);
    }

    private static Image getTestImage() {

        Map<String, Map<String, String>> imageSetsByProvider =
                Collections.singletonMap(TEST_PLATFORM, Collections.singletonMap("eu-west-1", "ami-03f39b6f019026862"));
        Map<String, String> packageVersions = Collections.singletonMap("cdh-build-number", "4217674");

        Map<String, String> repo = Collections.singletonMap("redhat7", "http://cloudera-build-us-west-1.vpc.cloudera.com/");

        Map<String, String> stackDetailsMap = Collections.singletonMap("repoid", "CDH-7.2.1");
        StackRepoDetails repoDetails = new StackRepoDetails(stackDetailsMap, Collections.emptyMap());
        StackDetails stackDetails = new StackDetails("7.2.1", repoDetails, "7.2.1-1.cdh7.2.1.p0.4217674");

        List<List<String>> preWarmParcels = List.of(List.of("PROFILER-2.0.3.2.0.3.0", "http://s3.amazonaws.com/dev.hortonworks.com/DSS/centos7/"));
        List<String> preWarmCsd = List.of("http://s3.amazonaws.com/dev.hortonworks.com/DSS/PROFILER-2.0.3.jar");

        Image image = new Image(TEST_DATE, TEST_CREATED, TEST_DESCRIPTION, TEST_OS, TEST_UUID, TEST_VERSION, repo, imageSetsByProvider, stackDetails,
                TEST_OS_TYPE, packageVersions, preWarmParcels, preWarmCsd, TEST_CM_BUILD_NUMBER, true, TEST_BASE_PARCEL_URL, TEST_SOURCE_IMAGE_ID);
        image.setDefaultImage(true);
        return image;
    }

    private static Image getTestImageWithoutCollections() {
        Image image = new Image(TEST_DATE, TEST_CREATED, TEST_DESCRIPTION, TEST_OS, TEST_UUID, TEST_VERSION, null, null, null,
                TEST_OS_TYPE, null, null, null, TEST_CM_BUILD_NUMBER, true, TEST_BASE_PARCEL_URL, TEST_SOURCE_IMAGE_ID);
        image.setDefaultImage(true);
        return image;
    }

    private void validateImageV4Response(ImageV4Response response, boolean emptyCollections) {
        assertEquals(TEST_DATE, response.getDate());
        assertEquals(TEST_CREATED, response.getCreated());
        assertEquals(TEST_DESCRIPTION, response.getDescription());
        assertEquals(TEST_OS, response.getOs());
        assertEquals(TEST_OS_TYPE, response.getOsType());
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