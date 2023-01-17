package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneRegistrationEnum.AKS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AzurePrivateDnsZoneRegistrationEnumTest {
    @Test
    void testNumberOfZoneTypes() {
        assertEquals(1, testServicesSource().count(), "Please add tests for missing enums");
    }

    @ParameterizedTest
    @MethodSource(value = "testServicesSource")
    void testRegistrationEnumValues(Pair<AzurePrivateDnsZoneRegistrationEnum, AzurePrivateDnsZoneRegistrationEnumValues> serviceEnumAndExpectedValues) {
        AzurePrivateDnsZoneRegistrationEnum serviceEnum = serviceEnumAndExpectedValues.getKey();
        AzurePrivateDnsZoneRegistrationEnumValues expectedValues = serviceEnumAndExpectedValues.getValue();

        assertEquals(expectedValues.getResourceType(), serviceEnum.getResourceType());
        assertEquals(expectedValues.getSubResource(), serviceEnum.getSubResource());
        assertEquals(expectedValues.getDnsZoneName(), serviceEnum.getDnsZoneName());
        List<String> dnsZoneNamePatterns = serviceEnum.getDnsZoneNamePatterns().stream().map(Pattern::pattern).collect(Collectors.toList());
        assertThat(dnsZoneNamePatterns).asList().hasSameElementsAs(expectedValues.getDnsZoneNameRegexPatterns());
    }

    private static Stream<Pair<AzurePrivateDnsZoneRegistrationEnum, AzurePrivateDnsZoneRegistrationEnumValues>> testServicesSource() {
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

        public AzurePrivateDnsZoneRegistrationEnumValues(String resourceType, String subResource, String dnsZoneName, List<String> dnsZoneNameRegexPatterns) {
            this.resourceType = resourceType;
            this.subResource = subResource;
            this.dnsZoneName = dnsZoneName;
            this.dnsZoneNameRegexPatterns = dnsZoneNameRegexPatterns;
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getSubResource() {
            return subResource;
        }

        public String getDnsZoneName() {
            return dnsZoneName;
        }

        public List<String> getDnsZoneNameRegexPatterns() {
            return dnsZoneNameRegexPatterns;
        }
    }

}
