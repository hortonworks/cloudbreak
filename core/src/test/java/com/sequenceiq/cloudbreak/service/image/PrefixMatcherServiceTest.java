package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class PrefixMatcherServiceTest {

    private static final String V2_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-image-catalog-v2.json";

    private static Collection<CloudbreakVersion> versions;

    @InjectMocks
    private PrefixMatcherService underTest;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @BeforeClass
    public static void beforeClass() throws IOException {
        versions = getVersions();
    }

    @Test
    public void testPrefixMatchForCBVersionWithUnreleasedVersion() {
        PrefixMatchImages actual = underTest.prefixMatchForCBVersion("2.1.0-dev.200", versions);

        assertTrue(actual.getvMImageUUIDs().contains("2.4.2.2-1-9a308050-4d5b-45a9-bfee-ce1cc444953d-2.5.0.1-265"));
        assertTrue(actual.getvMImageUUIDs().contains("2.5.0.2-65-5288855d-d7b9-4b90-b326-ab4b168cf581-2.6.0.1-145"));
        assertTrue(actual.getvMImageUUIDs().contains("f6e778fc-7f17-4535-9021-515351df3691"));
        assertTrue(actual.getvMImageUUIDs().contains("2.5.0.2-65-fe0ba14f-cb44-4573-ac8c-23cbc72bbd57-2.6.0.1-152"));
        assertTrue(actual.getvMImageUUIDs().contains("7aca1fa6-980c-44e2-a75e-3144b18a5993"));
        assertTrue(actual.getvMImageUUIDs().contains("9958938a-1261-48e2-aff9-dbcb2cebf6cd"));
        assertTrue(actual.getDefaultVMImageUUIDs().contains("f6e778fc-7f17-4535-9021-515351df3691"));
        assertTrue(actual.getDefaultVMImageUUIDs().contains("7aca1fa6-980c-44e2-a75e-3144b18a5993"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.1"));
        assertTrue(actual.getSupportedVersions().contains("2.0.0"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.100"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.2"));
    }

    @Test
    public void testPrefixMatchForCBVersionWithReleasedVersion() {
        PrefixMatchImages actual = underTest.prefixMatchForCBVersion("2.1.0", versions);

        assertNotNull(actual);
        assertTrue(actual.getvMImageUUIDs().contains("2.4.2.2-1-9a308050-4d5b-45a9-bfee-ce1cc444953d-2.5.0.1-265"));
        assertTrue(actual.getvMImageUUIDs().contains("2.5.0.2-65-5288855d-d7b9-4b90-b326-ab4b168cf581-2.6.0.1-145"));
        assertTrue(actual.getvMImageUUIDs().contains("f6e778fc-7f17-4535-9021-515351df3691"));
        assertTrue(actual.getvMImageUUIDs().contains("2.5.0.2-65-fe0ba14f-cb44-4573-ac8c-23cbc72bbd57-2.6.0.1-152"));
        assertTrue(actual.getvMImageUUIDs().contains("7aca1fa6-980c-44e2-a75e-3144b18a5993"));
        assertTrue(actual.getvMImageUUIDs().contains("9958938a-1261-48e2-aff9-dbcb2cebf6cd"));
        assertTrue(actual.getDefaultVMImageUUIDs().isEmpty());
        assertTrue(actual.getSupportedVersions().contains("2.0.0"));
        assertTrue(actual.getSupportedVersions().contains("2.1.0-dev.100"));
    }

    @Test
    public void testPrefixMatchForCBVersionWithRcVersion() {
        PrefixMatchImages actual = underTest.prefixMatchForCBVersion("1.16.4-rc.13", versions);

        assertTrue(actual.getvMImageUUIDs().contains("2.4.2.2-1-9e3ccdca-fa64-42eb-ab29-b1450767bbd8-2.5.0.1-265"));
        assertTrue(actual.getvMImageUUIDs().contains("2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2"));
        assertTrue(actual.getDefaultVMImageUUIDs().isEmpty());
        assertTrue(actual.getSupportedVersions().contains("1.16.4"));
        assertTrue(actual.getSupportedVersions().contains("2.0.0-rc.1"));
        assertTrue(actual.getSupportedVersions().contains("2.0.0-rc.2"));
    }

    private static Collection<CloudbreakVersion> getVersions() throws IOException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(V2_CATALOG_FILE);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        return catalog.getVersions().getCloudbreakVersions();
    }

}