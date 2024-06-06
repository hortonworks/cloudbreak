package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.upgrade.ImageComponentVersionsComparator;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class DistroXUpgradeResponseFilterServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final long CLUSTER_ID = 123L;

    @InjectMocks
    private DistroXUpgradeResponseFilterService underTest;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Spy
    private ImageComponentVersionsComparator imageComponentVersionsComparator;

    @Test
    void testFilterForLatestImagePerRuntimeAndOs() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", CENTOS7);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3);

        List<ImageInfoV4Response> actual = underTest.filterForLatestImagePerRuntimeAndOs(candidates, createImage(1L, "7.2.15", CENTOS7));

        assertEquals(2, actual.size());
        assertTrue(actual.contains(candidate2));
        assertTrue(actual.contains(candidate3));
    }

    @Test
    void testFilterForLatestImagePerRuntimeAndOsShouldReturnOnlyTheCentosImageIfARHEL8ImageIsAvailableWithTheSamePackages() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", RHEL8);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", CENTOS7);
        ImageInfoV4Response candidate4 = createImage(2L, "7.2.18", RHEL8);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3, candidate4);

        List<ImageInfoV4Response> actual = underTest.filterForLatestImagePerRuntimeAndOs(candidates, createImage(1L, "7.2.15", CENTOS7));

        assertEquals(3, actual.size());
        assertTrue(actual.contains(candidate1));
        assertTrue(actual.contains(candidate3));
        assertTrue(actual.contains(candidate4));
    }

    @Test
    void testFilterForLatestImagePerRuntimeAndOsShouldReturnAllImageIfARHEL8ImageIsAvailableWithTheSamePackages() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", RHEL8);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2);

        List<ImageInfoV4Response> actual = underTest.filterForLatestImagePerRuntimeAndOs(candidates, createImage(1L, "7.2.16", CENTOS7));

        assertEquals(2, actual.size());
        assertTrue(actual.contains(candidate1));
        assertTrue(actual.contains(candidate2));
    }

    @Test
    void testFilterForLatestImagePerRuntimeAndOsShouldReturnAllImageIfTheCurrentOsIsCentosAndOnlyRHEL8CandidatesAvailable() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", RHEL8);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", RHEL8);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", RHEL8);
        ImageInfoV4Response candidate4 = createImage(2L, "7.2.18", RHEL8);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3, candidate4);

        List<ImageInfoV4Response> actual = underTest.filterForLatestImagePerRuntimeAndOs(candidates, createImage(1L, "7.2.16", CENTOS7));

        assertEquals(3, actual.size());
        assertTrue(actual.contains(candidate2));
        assertTrue(actual.contains(candidate3));
        assertTrue(actual.contains(candidate4));
    }

    @Test
    void testFilterForLatestImagePerRuntimeAndOsShouldReturnAllImageIfTheCurrentOsIsRHEL8AndOnlyRHEL8CandidatesAvailable() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", RHEL8);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", RHEL8);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", RHEL8);
        ImageInfoV4Response candidate4 = createImage(2L, "7.2.18", RHEL8);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3, candidate4);

        List<ImageInfoV4Response> actual = underTest.filterForLatestImagePerRuntimeAndOs(candidates, createImage(1L, "7.2.16", CENTOS7));

        assertEquals(3, actual.size());
        assertTrue(actual.contains(candidate2));
        assertTrue(actual.contains(candidate3));
        assertTrue(actual.contains(candidate4));
    }

    @Test
    void testFilterForDatalakeVersion() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", CENTOS7);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(
                new SdxBasicView(null, null, "7.2.16", false, 1L, null)));

        List<ImageInfoV4Response> actual = underTest.filterForDatalakeVersion(ENVIRONMENT_CRN, createUpgradeV4Response("7.2.16", candidates));

        assertEquals(2, actual.size());
        assertTrue(actual.contains(candidate1));
        assertTrue(actual.contains(candidate2));
    }

    @Test
    void testFilterForDatalakeVersionShouldReturnEmptyListWhenThereIsNoCandidateImageWithTheSameDataLakeVersion() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", CENTOS7);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(
                new SdxBasicView(null, null, "7.2.15", false, 1L, null)));

        List<ImageInfoV4Response> actual = underTest.filterForDatalakeVersion(ENVIRONMENT_CRN, createUpgradeV4Response("7.2.15", candidates));

        assertTrue(actual.isEmpty());
    }

    @Test
    void testFilterForDatalakeVersionShouldReturnEmptyListWhenThereIsNoCandidateImage() {
        List<ImageInfoV4Response> actual = underTest.filterForDatalakeVersion(ENVIRONMENT_CRN, createUpgradeV4Response("7.2.15", Collections.emptyList()));

        assertTrue(actual.isEmpty());
        verifyNoInteractions(platformAwareSdxConnector);
    }

    @Test
    void testFilterForDatalakeVersionShouldReturnAllCandidatesWhenTheDatalakeStackNotPresent() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", CENTOS7);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.empty());

        List<ImageInfoV4Response> actual = underTest.filterForDatalakeVersion(ENVIRONMENT_CRN, createUpgradeV4Response("7.2.15", candidates));

        assertEquals(3, actual.size());
        assertTrue(actual.contains(candidate1));
        assertTrue(actual.contains(candidate2));
        assertTrue(actual.contains(candidate3));
    }

    @Test
    void testFilterForDatalakeVersionShouldReturnAllCandidatesWhenTheRuntimeVersionIsNotPresentForTheDataLake() {
        ImageInfoV4Response candidate1 = createImage(1L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate2 = createImage(2L, "7.2.16", CENTOS7);
        ImageInfoV4Response candidate3 = createImage(2L, "7.2.17", CENTOS7);
        List<ImageInfoV4Response> candidates = List.of(candidate1, candidate2, candidate3);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.empty());

        List<ImageInfoV4Response> actual = underTest.filterForDatalakeVersion(ENVIRONMENT_CRN, createUpgradeV4Response("7.2.15", candidates));

        assertEquals(3, actual.size());
        assertTrue(actual.contains(candidate1));
        assertTrue(actual.contains(candidate2));
        assertTrue(actual.contains(candidate3));
    }

    private UpgradeV4Response createUpgradeV4Response(String currentRuntimeVersion, List<ImageInfoV4Response> candidates) {
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setCurrent(createImage(1L, currentRuntimeVersion, CENTOS7));
        upgradeV4Response.setUpgradeCandidates(candidates);
        return upgradeV4Response;
    }

    private ImageInfoV4Response createImage(Long created, String cdpVersion, OsType os) {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCdp(cdpVersion);
        imageComponentVersions.setOs(os.getOs());
        imageComponentVersions.setParcelInfoResponseList(Collections.emptyList());
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setComponentVersions(imageComponentVersions);
        imageInfoV4Response.setCreated(created);
        return imageInfoV4Response;
    }
}