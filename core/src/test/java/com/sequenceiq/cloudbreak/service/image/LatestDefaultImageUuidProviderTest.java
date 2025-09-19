package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
public class LatestDefaultImageUuidProviderTest {

    private static final Set<ImageCatalogPlatform> SINGLE_PLATFORM = new HashSet<>(Set.of(imageCatalogPlatform("AWS")));

    private static final Set<ImageCatalogPlatform> MULTIPLE_PLATFORMS = new HashSet<>(Set.of(
            imageCatalogPlatform("AWS"),
            imageCatalogPlatform("AZURE"),
            imageCatalogPlatform("YARN")));

    private static final Map<String, Map<String, String>> IMAGE_SETS_BY_PROVIDER =
            Map.of(
                    "aws", Map.of(
                            "ap-northeast-1", "ami-03c40802e9db52480",
                            "ap-northeast-2", "ami-09b13f03cae650994"),
                    "azure", Map.of(
                            "West Europe", "https://sequenceiqwesteurope2.blob.core.windows.net/images/cb-cdh--2005201254.vhd",
                            "Central US", "https://sequenceiqcentralus2.blob.core.windows.net/images/cb-cdh--2005201254.vhd"),
                    "yarn", Map.of(
                            "default", "registry.eng.hortonworks.com/cloudbreak/centos-76:2020-05-18-17-16-16"));

    private static List<Image> defaultImages;

    @Mock
    private ImageOsService imageOsService;

    private LatestDefaultImageUuidProvider underTest;

    @BeforeAll
    public static void beforeClass() {
        defaultImages = createTestImageList();
    }

    @BeforeEach
    void setUp() {
        lenient().when(imageOsService.getPreferredOs()).thenReturn("centos7");
        ImageComparator comparator = new ImageComparator();
        ReflectionTestUtils.setField(comparator, "imageOsService", imageOsService);
        underTest = new LatestDefaultImageUuidProvider(comparator);
    }

    @Test
    public void testLatestDefaultImageUuidsWithSinglePlatform() {
        Collection<String> actual = underTest.getLatestDefaultImageUuids(SINGLE_PLATFORM, defaultImages);
        assertThat(actual).containsExactlyInAnyOrder(
                "aws-centos7-1",
                "aws-centos7-4",
                "aws-centos7-5",
                "aws-centos7-7",
                "aws-redhat8-1",
                "aws-redhat8arm64-1"
        );
    }

    @Test
    public void testLatestDefaultImageUuidsWithMultiplePlatforms() {
        Collection<String> actual = underTest.getLatestDefaultImageUuids(MULTIPLE_PLATFORMS, defaultImages);
        assertThat(actual).containsExactlyInAnyOrder(
                "aws-centos7-1",
                "aws-centos7-4",
                "aws-centos7-5",
                "aws-centos7-7",
                "azure-centos7-1",
                "azure-centos7-2",
                "yarn-centos7-1",

                "aws-redhat8-1",
                "azure-redhat8-1",
                "yarn-redhat8-1",

                "aws-redhat8arm64-1"
        );
    }

    @Test
    public void testLatestDefaultImageUuidsWithEmptyDefaultImageList() {
        Collection<String> actual = underTest.getLatestDefaultImageUuids(SINGLE_PLATFORM, Collections.emptyList());
        assertThat(actual).isEmpty();
    }

    private static Image createMockImage(String uuid, String provider, String version, String os, Architecture architecture, String date, Long created) {
        Image image = mock(Image.class);
        when(image.getUuid()).thenReturn(uuid);
        when(image.getVersion()).thenReturn(version);
        when(image.getOs()).thenReturn(os);
        when(image.getArchitecture()).thenReturn(architecture.getName());
        lenient().when(image.getDate()).thenReturn(date);
        when(image.getCreated()).thenReturn(created);
        when(image.getImageSetsByProvider()).thenReturn(Map.of(provider, IMAGE_SETS_BY_PROVIDER.get(provider)));
        return image;
    }

    private static List<Image> createTestImageList() {
        return List.of(
                // -- centos7
                // Aws
                createMockImage("aws-centos7-1", "aws", null, "centos7", Architecture.X86_64, "2020-06-08", Long.valueOf(1589562459)),
                createMockImage("aws-centos7-2", "aws", null, "centos7", Architecture.X86_64, "2020-03-12", Long.valueOf(1513861351)),
                createMockImage("aws-centos7-3", "aws", "7.0.2", "centos7", Architecture.X86_64, "2020-04-01", Long.valueOf(1521268445)),
                createMockImage("aws-centos7-4", "aws", "7.0.2", "centos7", Architecture.X86_64, "2020-04-11", Long.valueOf(1522563401)),
                createMockImage("aws-centos7-5", "aws", "7.1.0", "centos7", Architecture.X86_64, "2020-05-15", Long.valueOf(1589562459)),
                createMockImage("aws-centos7-6", "aws", "7.1.0", "centos7", Architecture.X86_64, "2020-05-14", Long.valueOf(1589561390)),
                createMockImage("aws-centos7-7", "aws", "7.2.0", "centos7", Architecture.X86_64, "2020-06-24", Long.valueOf(1934560145)),
                createMockImage("aws-centos7-8", "aws", "7.2.0", "centos7", Architecture.X86_64, "2020-06-15", Long.valueOf(1913560024)),
                createMockImage("aws-centos7-9", "aws", "7.2.0", "centos7", Architecture.X86_64, "2020-05-03", Long.valueOf(1624441323)),
                // Azure
                createMockImage("azure-centos7-1", "azure", "7.1.0", "centos7", Architecture.X86_64, "2020-05-23", null),
                createMockImage("azure-centos7-2", "azure", "7.2.0", "centos7", Architecture.X86_64, "2020-06-18", null),
                createMockImage("azure-centos7-3", "azure", "7.2.0", "centos7", Architecture.X86_64, "2019-05-01", null),
                createMockImage("azure-centos7-4", "azure", "7.2.0", "centos7", Architecture.X86_64, "2020-06-12", null),
                //yCloud
                createMockImage("yarn-centos7-1", "yarn", "7.2.0", "centos7", Architecture.X86_64, "2020-06-04", Long.valueOf(1834001260)),
                // -- redhat8
                // Aws
                createMockImage("aws-redhat8-1", "aws", "7.2.0", "redhat8", Architecture.X86_64, "2020-06-24", Long.valueOf(1934560145)),
                createMockImage("aws-redhat8-2", "aws", "7.2.0", "redhat8", Architecture.X86_64, "2020-06-15", Long.valueOf(1913560024)),
                createMockImage("aws-redhat8-3", "aws", "7.2.0", "redhat8", Architecture.X86_64, "2020-05-03", Long.valueOf(1624441323)),
                // Azure
                createMockImage("azure-redhat8-1", "azure", "7.2.0", "redhat8", Architecture.X86_64, "2020-06-24", Long.valueOf(1934560145)),
                createMockImage("azure-redhat8-2", "azure", "7.2.0", "redhat8", Architecture.X86_64, "2020-06-15", Long.valueOf(1913560024)),
                createMockImage("azure-redhat8-3", "azure", "7.2.0", "redhat8", Architecture.X86_64, "2020-05-03", Long.valueOf(1624441323)),
                //yCloud
                createMockImage("yarn-redhat8-1", "yarn", "7.2.0", "redhat8", Architecture.X86_64, "2020-06-04", Long.valueOf(1834001260)),
                // -- redhat8-arm64
                // Aws
                createMockImage("aws-redhat8arm64-1", "aws", "7.2.0", "redhat8", Architecture.ARM64, "2020-06-24", Long.valueOf(1934560145))
        );
    }
}
