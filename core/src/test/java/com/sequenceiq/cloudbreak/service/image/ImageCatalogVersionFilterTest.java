package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;

public class ImageCatalogVersionFilterTest {

    private ImageCatalogVersionFilter underTest = new ImageCatalogVersionFilter();

    private String unspecifiedVersion = "unspecified";

    private String unReleasedVersion = " 2.6.0-rc.13";

    private String devVersion = "2.6.0-dev.132";

    private String extendedUnreleasedVersion = "2.8.0-rev.82";

    private String releasedVersion = "2.7.0";

    private String latestRcVersion = "2.4.0-rc.13";

    private String rEVersionOlder = "2.6.0-b14";

    private String rEVersionNewest = "2.8.0-b83";

    private List<CloudbreakVersion> versions;

    @Test
    public void getLatestCloudbreakVersion() {
        List<String> list1 = generateVersionList(devVersion, unReleasedVersion);
        List<String> list2 = generateVersionList(releasedVersion, extendedUnreleasedVersion);
        versions = generateCloudBreakVersions(list1, list2);
        assertEquals("2.8.0-rev.82", underTest.latestCloudbreakVersion(versions));
    }

    @Test
    public void getLatestCloudbreakVersionWhenREVersioningExistsAndNewest() {
        List<String> list1 = generateVersionList(devVersion, unReleasedVersion);
        List<String> list2 = generateVersionList(releasedVersion, extendedUnreleasedVersion);
        List<String> rEVersionList = generateVersionList(rEVersionOlder, rEVersionNewest);
        versions = generateCloudBreakVersions(list1, list2, rEVersionList);

        String actual = underTest.latestCloudbreakVersion(versions);

        assertEquals("2.8.0-b83", actual);
    }

    @Test
    public void getLatestCloudbreakVersionWhenREVersioningExistsAndNotNewest() {
        List<String> list1 = generateVersionList(devVersion, unReleasedVersion);
        List<String> list2 = generateVersionList(rEVersionOlder, extendedUnreleasedVersion);
        versions = generateCloudBreakVersions(list1, list2);

        String actual = underTest.latestCloudbreakVersion(versions);

        assertEquals("2.8.0-rev.82", actual);
    }

    @Test
    public void filterUnreleasedVersions() {
        List<String> list1 = generateVersionList(devVersion, unReleasedVersion);
        List<String> list2 = generateVersionList(releasedVersion, extendedUnreleasedVersion);
        versions = generateCloudBreakVersions(list1, list2);
        assertEquals(2, versions.size());
        assertEquals(1, underTest.filterUnreleasedVersions(versions, unReleasedVersion).size());
    }

    @Test
    public void isVersionUnspecified() {
        assertTrue(underTest.isVersionUnspecified(unspecifiedVersion));
        assertFalse(underTest.isVersionUnspecified(unReleasedVersion));
    }

    @Test
    public void extractUnreleasedVersion() {
        String extractedVersion = underTest.extractUnreleasedVersion(unReleasedVersion);
        assertEquals(" 2.6.0-rc.13", extractedVersion);
    }

    @Test
    public void extractReleasedVersion() {
        assertEquals("2.7.0", underTest.extractReleasedVersion(releasedVersion));
    }

    @Test
    public void extractReleaseVersionFromDev() {
        assertEquals("2.6.0", underTest.extractReleasedVersion(devVersion));
    }

    @Test
    public void extractReleaseVersionFromRc() {
        assertEquals("2.4.0", underTest.extractReleasedVersion(latestRcVersion));
    }

    @Test
    public void extractReleasedFallbackOriginalVersion() {
        String extractedVersion = underTest.extractReleasedVersion(unReleasedVersion);
        assertEquals(unReleasedVersion, extractedVersion);
    }

    @Test
    public void extractExtendedUnreleasedVersion() {
        String extractedVersion = underTest.extractExtendedUnreleasedVersion(extendedUnreleasedVersion);
        assertEquals(extendedUnreleasedVersion, extractedVersion);
    }

    private List<CloudbreakVersion> generateCloudBreakVersions(List<String>... cbVersions) {
        List<CloudbreakVersion> result = new ArrayList<>();
        for (List<String> list: cbVersions) {
            result.add(new CloudbreakVersion(list, null, null));
        }
        return result;
    }

    private static List<String> generateVersionList(String... ver) {
        List<String> result = new ArrayList<>();
        for (String v: ver) {
            result.add(v);
        }
        return result;
    }
}