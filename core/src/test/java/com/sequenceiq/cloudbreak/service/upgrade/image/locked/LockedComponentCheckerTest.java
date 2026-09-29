package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class LockedComponentCheckerTest {

    private static final String CM_BUILD_NUMBER = "12345";

    private final Image candidateImage = mock(Image.class);

    private final Map<String, String> activatedParcels = Map.of();

    @Mock
    private ParcelMatcher parcelMatcher;

    @Mock
    private StackVersionMatcher stackVersionMatcher;

    @Mock
    private CmVersionMatcher cmVersionMatcher;

    @InjectMocks
    private LockedComponentChecker underTest;

    static Object[][] parameters() {
        return new Object[][]{
                {Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE},
                {Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE},
                {Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE},
                {Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE}
        };
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testResult(Boolean expectedResult, Boolean parcelMatching, Boolean stackVersionMatching, Boolean cmVersionMatching) {
        when(parcelMatcher.isMatchingNonCdhParcels(candidateImage, activatedParcels)).thenReturn(parcelMatching);
        when(stackVersionMatcher.isMatchingStackVersion(candidateImage, activatedParcels)).thenReturn(stackVersionMatching);
        when(cmVersionMatcher.isCmVersionMatching(CM_BUILD_NUMBER, candidateImage)).thenReturn(cmVersionMatching);

        boolean result = underTest.isUpgradePermitted(candidateImage, activatedParcels, CM_BUILD_NUMBER);

        assertEquals(expectedResult, result);
        verify(parcelMatcher, times(1)).isMatchingNonCdhParcels(candidateImage, activatedParcels);
        verify(stackVersionMatcher, times(1)).isMatchingStackVersion(candidateImage, activatedParcels);
        verify(cmVersionMatcher, times(1)).isCmVersionMatching(CM_BUILD_NUMBER, candidateImage);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testResultFromUpgradeProperties(Boolean expectedResult, Boolean parcelMatching, Boolean stackVersionMatching, Boolean cmVersionMatching) {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName("CDH").withVersion("7.2.18");
        ClouderaManagerProduct spark = new ClouderaManagerProduct().withName("SPARK3").withVersion("3.0");
        ClouderaManagerRepo repo = new ClouderaManagerRepo();
        repo.setBuildNumber("target-build");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.2.18", "base-image", OsType.RHEL8, "x86_64", cdh, Set.of(spark), repo);
        properties.getCurrentPackageVersions().put(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), CM_BUILD_NUMBER);
        when(parcelMatcher.isMatchingNonCdhParcels(Set.of(cdh, spark), activatedParcels)).thenReturn(parcelMatching);
        when(stackVersionMatcher.isMatchingStackVersion(cdh, activatedParcels)).thenReturn(stackVersionMatching);
        when(cmVersionMatcher.isCmVersionMatching(CM_BUILD_NUMBER, repo)).thenReturn(cmVersionMatching);

        assertEquals(expectedResult, underTest.isUpgradePermitted(properties, activatedParcels));

        verify(parcelMatcher).isMatchingNonCdhParcels(Set.of(cdh, spark), activatedParcels);
        verify(stackVersionMatcher).isMatchingStackVersion(cdh, activatedParcels);
        verify(cmVersionMatcher).isCmVersionMatching(CM_BUILD_NUMBER, repo);
    }
}