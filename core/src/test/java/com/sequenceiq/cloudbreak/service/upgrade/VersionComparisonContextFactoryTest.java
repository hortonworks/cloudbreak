package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.util.CdhPatchVersionProvider;

@ExtendWith(MockitoExtension.class)
class VersionComparisonContextFactoryTest {

    private static final String VERSION = "7.1.0";

    private static final Integer BUILD_NUMBER = 123;

    private static final int PATCH_VERSION = 100;

    @Mock
    private CdhPatchVersionProvider cdhPatchVersionProvider;

    @InjectMocks
    private VersionComparisonContextFactory underTest;

    @Test
    void testBuildForCm() {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.CM.getKey(), VERSION);
        packageVersions.put(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), BUILD_NUMBER.toString());

        VersionComparisonContext context = underTest.buildForCm(packageVersions);

        assertEquals(VERSION, context.getMajorVersion());
        assertEquals(BUILD_NUMBER, context.getBuildNumber());
    }

    @Test
    void testBuildForStackWithImage() {
        String fullVersion = VERSION + "-" + PATCH_VERSION;
        Image image = Image.builder()
                .withVersion(VERSION)
                .withStackDetails(new ImageStackDetails(null, new StackRepoDetails(Map.of(REPOSITORY_VERSION, fullVersion), null), null))
                .withPackageVersions(Map.of(ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), BUILD_NUMBER.toString()))
                .build();
        when(cdhPatchVersionProvider.getPatchFromVersionString(fullVersion)).thenReturn(Optional.of(PATCH_VERSION));

        VersionComparisonContext context = underTest.buildForStack(image);

        assertEquals(VERSION, context.getMajorVersion());
        assertEquals(Optional.of(PATCH_VERSION), context.getPatchVersion());
        assertEquals(BUILD_NUMBER, context.getBuildNumber());
    }

    @Test
    void testBuildForStackWithMaps() {
        String fullVersion = VERSION + "-" + PATCH_VERSION;
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.STACK.getKey(), VERSION);
        packageVersions.put(ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), BUILD_NUMBER.toString());

        Map<String, String> stackRelatedParcels = new HashMap<>();
        stackRelatedParcels.put("CDH", fullVersion);

        when(cdhPatchVersionProvider.getPatchFromVersionString(fullVersion)).thenReturn(Optional.of(PATCH_VERSION));

        VersionComparisonContext context = underTest.buildForStack(packageVersions, stackRelatedParcels);

        assertEquals(VERSION, context.getMajorVersion());
        assertEquals(Optional.of(PATCH_VERSION), context.getPatchVersion());
        assertEquals(BUILD_NUMBER, context.getBuildNumber());
    }
}