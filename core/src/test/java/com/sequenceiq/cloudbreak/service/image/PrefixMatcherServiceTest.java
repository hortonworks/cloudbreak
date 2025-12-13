package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class PrefixMatcherServiceTest {

    private static final String CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-image-catalog-v2.json";

    private static final String RC_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-rc-image-catalog.json";

    private static Collection<CloudbreakVersion> versionsFromV2Catalog;

    private static Collection<CloudbreakVersion> versionsFromV3Catalog;

    @InjectMocks
    private PrefixMatcherService underTest;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @BeforeAll
    static void beforeClass() throws IOException {
        versionsFromV2Catalog = getVersions(CATALOG_FILE);
        versionsFromV3Catalog = getVersions(RC_CATALOG_FILE);
    }

    @Test
    void testPrefixMatchForCBVersionWithUnreleasedVersion() {
        PrefixMatchImages actual = underTest.prefixMatchForCBVersion("2.1.0-dev.200", versionsFromV2Catalog);

        assertTrue(actual.getvMImageUUIDs().contains("f6e778fc-7f17-4535-9021-515351df3691"));
        assertTrue(actual.getvMImageUUIDs().contains("7aca1fa6-980c-44e2-a75e-3144b18a5993"));
        assertTrue(actual.getDefaultVMImageUUIDs().contains("f6e778fc-7f17-4535-9021-515351df3691"));
        assertTrue(actual.getDefaultVMImageUUIDs().contains("7aca1fa6-980c-44e2-a75e-3144b18a5993"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.1"));
        assertTrue(actual.getSupportedVersions().contains("2.0.0"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.100"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.2"));
    }

    @Test
    void testPrefixMatchForCBVersionWithReleasedVersion() {
        PrefixMatchImages actual = underTest.prefixMatchForCBVersion("2.1.0", versionsFromV2Catalog);

        assertNotNull(actual);
        assertTrue(actual.getvMImageUUIDs().contains("f6e778fc-7f17-4535-9021-515351df3691"));
        assertTrue(actual.getvMImageUUIDs().contains("7aca1fa6-980c-44e2-a75e-3144b18a5993"));
        assertTrue(actual.getDefaultVMImageUUIDs().isEmpty());
        assertTrue(actual.getSupportedVersions().contains("2.0.0"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.100"));
    }

    @Test
    void testPrefixMatchForCBVersionWithRcVersion() {
        PrefixMatchImages actual = underTest.prefixMatchForCBVersion("2.7.0-rc.2", versionsFromV3Catalog);

        assertTrue(actual.getvMImageUUIDs().contains("d4d57241-0be9-4ebf-9e00-5baea7bbed49"));
        assertTrue(actual.getvMImageUUIDs().contains("0f575e42-9d90-4f85-5f8a-bdced2221dc3"));
        assertTrue(actual.getvMImageUUIDs().contains("233a3a99-023d-468d-44d4-2d1330e81bab"));
        assertTrue(actual.getDefaultVMImageUUIDs().contains("d4d57241-0be9-4ebf-9e00-5baea7bbed49"));
        assertTrue(actual.getDefaultVMImageUUIDs().contains("0f575e42-9d90-4f85-5f8a-bdced2221dc3"));
        assertTrue(actual.getSupportedVersions().contains("2.7.0-rc.1"));
        assertTrue(actual.getSupportedVersions().contains("2.7.0-rc.2"));
    }

    private static Collection<CloudbreakVersion> getVersions(String catalogFilePath) throws IOException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFilePath);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        return catalog.getVersions().getCloudbreakVersions();
    }

}