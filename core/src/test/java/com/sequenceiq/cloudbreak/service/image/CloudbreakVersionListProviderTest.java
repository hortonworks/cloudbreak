package com.sequenceiq.cloudbreak.service.image;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;

@ExtendWith(MockitoExtension.class)
class CloudbreakVersionListProviderTest {

    private static final List<String> CLOUDBREAK_VERSIONS =
            List.of("2.39.0-b124", "2.39.0-b147", "2.40.0-b143", "2.40.0-b163", "2.41.0-b110");

    private static final List<String> CLOUDBREAK_DEFAULTS =
            List.of("2a987115-b5b5-4c68-7ed5-a474e20a3f98", "2dde06c3-ae6e-4452-604c-a8bebea71edb", "d558405b-b8ba-4425-94cc-a8baff9ffb2c");

    private static final List<String> CLOUDBREAK_IMAGE_IDS =
            List.of("d558405b-b8ba-4425-94cc-a8baff9ffb2c", "a779c9a1-fd34-4b69-4951-a848d1883b99", "92473bb8-2137-431f-7c4c-d90752ce2a5c");

    private static final List<String> FREEIPA_VERSIONS =
            List.of("2.35.0-b50", "2.38.0-b102", "2.41.0-b28");

    private static final List<String> FREEIPA_IMAGE_IDS =
            List.of("50d04f27-c2d2-471f-bf5a-02882677216c", "f118596f-f1a0-4548-b012-99c1b212e812");

    @InjectMocks
    private CloudbreakVersionListProvider underTest;

    @Test
    void testGetVersionsWithCloudbreakVersions() {

        CloudbreakImageCatalogV3 catalog = new CloudbreakImageCatalogV3(null, new Versions(cloudbreakVersions(), null));
        List<CloudbreakVersion> versions = underTest.getVersions(catalog);

        assertEquals(1, versions.size());
        assertEquals(CLOUDBREAK_VERSIONS, versions.get(0).getVersions());
        assertEquals(CLOUDBREAK_IMAGE_IDS, versions.get(0).getImageIds());
        assertEquals(CLOUDBREAK_DEFAULTS, versions.get(0).getDefaults());
    }

    @Test
    void testGetVersionsWithFreeipaVersions() {

        CloudbreakImageCatalogV3 catalog = new CloudbreakImageCatalogV3(null, new Versions(null, freeipaVersions()));
        List<CloudbreakVersion> versions = underTest.getVersions(catalog);

        assertEquals(1, versions.size());
        assertEquals(FREEIPA_VERSIONS, versions.get(0).getVersions());
        assertEquals(FREEIPA_IMAGE_IDS, versions.get(0).getImageIds());
        assertTrue(versions.get(0).getDefaults().isEmpty());
    }

    @Test
    void testGetVersionsWithNullCatalog() {

        List<CloudbreakVersion> versions = underTest.getVersions(null);

        assertNotNull(versions);
        assertTrue(versions.isEmpty());
    }

    @Test
    void testGetVersionsWithNullCatalogVersions() {

        CloudbreakImageCatalogV3 catalog = new CloudbreakImageCatalogV3(null, null);
        List<CloudbreakVersion> versions = underTest.getVersions(catalog);

        assertNotNull(versions);
        assertTrue(versions.isEmpty());
    }

    private List<CloudbreakVersion> cloudbreakVersions() {
        return asList(new CloudbreakVersion(CLOUDBREAK_VERSIONS, CLOUDBREAK_DEFAULTS, CLOUDBREAK_IMAGE_IDS));
    }

    private List<CloudbreakVersion> freeipaVersions() {
        return asList(new CloudbreakVersion(FREEIPA_VERSIONS, null, FREEIPA_IMAGE_IDS));
    }
}