package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ParcelInfoResponse;

class ImageComponentVersionsComparatorTest {

    private final ImageComponentVersionsComparator underTest = new ImageComponentVersionsComparator();

    private static Object[][] scenarios() {
        return new Object[][] {
                { "7.2.16", "123", "7.3.0", "345", List.of(new ParcelInfoResponse("cfm", null, "780")) },
                { "7.2.17", "124", "7.3.0", "345", List.of(new ParcelInfoResponse("cfm", null, "780")) },
                { "7.2.17", "123", "7.3.1", "345", List.of(new ParcelInfoResponse("cfm", null, "780")) },
                { "7.2.17", "123", "7.3.0", "346", List.of(new ParcelInfoResponse("cfm", null, "780")) },
                { "7.2.17", "123", "7.3.0", "345", List.of(new ParcelInfoResponse("cfm", null, "781")) },
        };
    }

    @Test
    void testContainsSamePackagesShouldReturnTrue() {
        ImageComponentVersions image1 = createImageComponentVersions("7.2.17", "123", "7.3.0", "345", List.of(new ParcelInfoResponse("cfm", null, "780")));
        ImageComponentVersions image2 = createImageComponentVersions("7.2.17", "123", "7.3.0", "345", List.of(new ParcelInfoResponse("cfm", null, "780")));

        assertTrue(underTest.containsSamePackages(image1, image2));
    }

    @ParameterizedTest(name = "cdhVersion: {0}, cdhBuildNumber: {1}, cmVersion: {2}, cmBuildNumber: {3}, parcelVersions: {4}")
    @MethodSource("scenarios")
    void testContainsSamePackagesShouldReturnFalse(String cdhVersion, String cdhBuildNumber, String cmVersion, String cmBuildNumber,
            List<ParcelInfoResponse> parcelVersions) {
        ImageComponentVersions image1 = createImageComponentVersions(cdhVersion, cdhBuildNumber, cmVersion, cmBuildNumber, parcelVersions);
        ImageComponentVersions image2 = createImageComponentVersions("7.2.17", "123", "7.3.0", "345", List.of(new ParcelInfoResponse("cfm", null, "780")));

        assertFalse(underTest.containsSamePackages(image1, image2));
    }

    private ImageComponentVersions createImageComponentVersions(String cdhVersion, String cdhBuildNumber, String cmVersion, String cmBuildNumber,
            List<ParcelInfoResponse> parcelVersions) {
        return new ImageComponentVersions(cmVersion, cmBuildNumber, cdhVersion, cdhBuildNumber, null, null, parcelVersions);
    }
}