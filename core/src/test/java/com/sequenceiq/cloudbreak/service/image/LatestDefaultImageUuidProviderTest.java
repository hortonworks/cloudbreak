package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class LatestDefaultImageUuidProviderTest {

    private static final Set<String> SINGLE_PLATFORM = new HashSet<>(Set.of("AWS"));

    private static final Set<String> MULTIPLE_PLATFORMS = new HashSet<>(Set.of("AWS", "AZURE", "YARN"));

    private static final Map<String, Map<String, String>> AWS_IMAGE_SET_BY_PROVIDER =
            Map.of("aws", Map.of("ap-northeast-1", "ami-03c40802e9db52480", "ap-northeast-2", "ami-09b13f03cae650994"));

    private static final Map<String, Map<String, String>> AZURE_IMAGE_SET_BY_PROVIDER =
            Map.of("azure", Map.of("West Europe", "https://sequenceiqwesteurope2.blob.core.windows.net/images/cb-cdh--2005201254.vhd",
                    "Central US", "https://sequenceiqcentralus2.blob.core.windows.net/images/cb-cdh--2005201254.vhd"));

    private static final Map<String, Map<String, String>> YARN_IMAGE_SET_BY_PROVIDER =
            Map.of("yarn", Map.of("default", "registry.eng.hortonworks.com/cloudbreak/centos-76:2020-05-18-17-16-16"));

    private static LatestDefaultImageUuidProvider underTest;

    private static List<Image> defaultImages;

    @BeforeClass
    public static void beforeClass() {
        underTest = new LatestDefaultImageUuidProvider(new ImageComparator());
        defaultImages = createTestImageList();
    }

    @Test
    public void testLatestDefaultImageUuidsWithSingelPlatform() {
        Collection<String> actual = underTest.getLatestDefaultImageUuids(SINGLE_PLATFORM, defaultImages);
        assertEquals(4, actual.size());
        assertTrue(actual.containsAll(List.of("id1", "id4", "id5", "id7")));
    }

    @Test
    public void testLatestDefaultImageUuidsWithMultiplePlatforms() {
        Collection<String> actual = underTest.getLatestDefaultImageUuids(MULTIPLE_PLATFORMS, defaultImages);
        assertEquals(7, actual.size());
        assertTrue(actual.containsAll(List.of("id1", "id4", "id5", "id7", "id10", "id11", "id14")));
    }

    @Test
    public void testLatestDefaultImageUuidsWithEmptyDefaultImageList() {
        Collection<String> actual = underTest.getLatestDefaultImageUuids(SINGLE_PLATFORM, Collections.emptyList());
        assertTrue(actual.isEmpty());
    }

    private static Image createMockImage(String uuid, String version, String date, Long created, Map<String, Map<String, String>> imageSetByProvider) {
        Image image = mock(Image.class);
        when(image.getUuid()).thenReturn(uuid);
        when(image.getVersion()).thenReturn(version);
        lenient().when(image.getDate()).thenReturn(date);
        when(image.getCreated()).thenReturn(created);
        when(image.getImageSetsByProvider()).thenReturn(imageSetByProvider);
        return image;
    }

    private static List<Image> createTestImageList() {
        return new ArrayList<>(List.of(
                // Aws
                createMockImage("id1", null, "2020-06-08", Long.valueOf(1589562459), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id2", null, "2020-03-12", Long.valueOf(1513861351), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id3", "7.0.2", "2020-04-01", Long.valueOf(1521268445), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id4", "7.0.2", "2020-04-11", Long.valueOf(1522563401), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id5", "7.1.0", "2020-05-15", Long.valueOf(1589562459), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id6", "7.1.0", "2020-05-14", Long.valueOf(1589561390), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id7", "7.2.0", "2020-06-24", Long.valueOf(1934560145), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id8", "7.2.0", "2020-06-15", Long.valueOf(1913560024), AWS_IMAGE_SET_BY_PROVIDER),
                createMockImage("id9", "7.2.0", "2020-05-03", Long.valueOf(1624441323), AWS_IMAGE_SET_BY_PROVIDER),
                // Azure
                createMockImage("id10", "7.1.0", "2020-05-23", null, AZURE_IMAGE_SET_BY_PROVIDER),
                createMockImage("id11", "7.2.0", "2020-06-18", null, AZURE_IMAGE_SET_BY_PROVIDER),
                createMockImage("id12", "7.2.0", "2019-05-01", null, AZURE_IMAGE_SET_BY_PROVIDER),
                createMockImage("id13", "7.2.0", "2020-06-12", null, AZURE_IMAGE_SET_BY_PROVIDER),
                //yCloud
                createMockImage("id14", "7.2.0", "2020-06-04", Long.valueOf(1834001260), YARN_IMAGE_SET_BY_PROVIDER))
        );
    }
}
