package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureRegisteredPrivateDnsZoneServiceType.AKS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AzureRegisteredPrivateDnsZoneServiceTypeTest {
    @Test
    void testNumberOfZoneTypes() {
        assertEquals(1, testServicesSource().count(), "Please add tests for missing enums");
    }

    @Test
    void testZoneNamePatternCount() {
        assertThat(AKS.getDnsZoneNamePatterns()).asList().hasSize(2);
    }

    @ParameterizedTest
    @MethodSource("testZoneNamePatterns")
    void testPatterns(AzureRegisteredPrivateDnsZoneServiceType serviceEnum, String testZoneName, Boolean shouldMatch) {
        boolean zoneNameMatchedByPattern = serviceEnum.getDnsZoneNamePatterns().stream()
                .map(pattern -> pattern.matcher(testZoneName))
                .anyMatch(Matcher::matches);
        assertEquals(shouldMatch, zoneNameMatchedByPattern);
    }

    private static Object[][] testZoneNamePatterns() {
        return new Object[][]{
                {AKS, "privatelink.region.azmk8s.io", true},
                {AKS, "privatelink.region2.azmk8s.io", true},
                {AKS, "privatelink.region-2.azmk8s.io", true},
                {AKS, "PrivateLink.REGION-2.Azmk8s.Io", false},
                {AKS, "region-2.azmk8s.io", false},
                {AKS, "privatelink.region-2.azmk8s.badpart", false},
                {AKS, "privatelink.region-2.badpart.io", false},
                {AKS, "badpart.region-2.amzk8s.io", false},

                {AKS, "mysubzonename.privatelink.region.azmk8s.io", true},
                {AKS, "mySubZoneName.privatelink.region.azmk8s.io", true},
                {AKS, "MY-SUB-ZONE-NAME.privatelink.region.azmk8s.io", true},
                {AKS, "mysubsubzonename.mysubzonename.privatelink.region.azmk8s.io", false},
                {AKS, "mysubzonename.privatelink.region.azmk8s.badpart", false},
                {AKS, "mysubzonename.privatelink.region.badpart.io", false},
                {AKS, "mysubzonename.badpart.region.azmk8s.io", false},

        };
    }

    @ParameterizedTest
    @MethodSource(value = "testServicesSource")
    void testRegistrationEnumValues(Pair<AzureRegisteredPrivateDnsZoneServiceType, AzurePrivateDnsZoneRegistrationEnumValues> serviceEnumAndExpectedValues) {
        AzureRegisteredPrivateDnsZoneServiceType serviceEnum = serviceEnumAndExpectedValues.getKey();
        AzurePrivateDnsZoneRegistrationEnumValues expectedValues = serviceEnumAndExpectedValues.getValue();

        assertEquals(expectedValues.getResourceType(), serviceEnum.getResourceType());
        assertEquals(expectedValues.getSubResource(), serviceEnum.getSubResource());
        assertEquals(expectedValues.getDnsZoneName(), serviceEnum.getDnsZoneName("aResourceGroup"));
        List<String> dnsZoneNamePatterns = serviceEnum.getDnsZoneNamePatterns().stream().map(Pattern::pattern).collect(Collectors.toList());
        assertThat(dnsZoneNamePatterns).asList().hasSameElementsAs(expectedValues.getDnsZoneNameRegexPatterns());
    }

    private static Stream<Pair<AzureRegisteredPrivateDnsZoneServiceType, AzurePrivateDnsZoneRegistrationEnumValues>> testServicesSource() {
        return Stream.of(
                Pair.of(AKS, new AzurePrivateDnsZoneRegistrationEnumValues(
                        "Microsoft.ContainerService/managedClusters",
                        "managedClusters",
                        "privatelink.{region}.azmk8s.io or {subzone}.privatelink.{region}.azmk8s.io",
                        List.of("privatelink\\.[a-z\\d-]+.azmk8s.io", "[\\w+-]+\\.privatelink\\.[a-z\\d-]+.azmk8s.io")))
        );
    }

    private static class AzurePrivateDnsZoneRegistrationEnumValues {
        private final String resourceType;

        private final String subResource;

        private final String dnsZoneName;

        private final List<String> dnsZoneNameRegexPatterns;

        private AzurePrivateDnsZoneRegistrationEnumValues(String resourceType, String subResource, String dnsZoneName, List<String> dnsZoneNameRegexPatterns) {
            this.resourceType = resourceType;
            this.subResource = subResource;
            this.dnsZoneName = dnsZoneName;
            this.dnsZoneNameRegexPatterns = dnsZoneNameRegexPatterns;
        }

        private String getResourceType() {
            return resourceType;
        }

        private String getSubResource() {
            return subResource;
        }

        private String getDnsZoneName() {
            return dnsZoneName;
        }

        private List<String> getDnsZoneNameRegexPatterns() {
            return dnsZoneNameRegexPatterns;
        }
    }

}