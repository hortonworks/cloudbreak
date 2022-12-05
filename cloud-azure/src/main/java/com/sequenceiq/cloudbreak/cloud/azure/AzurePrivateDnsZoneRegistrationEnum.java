package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This enum contains possible private DNS zones that are only registered in environment service, but are not used by cloudbreak.
 */
public enum AzurePrivateDnsZoneRegistrationEnum implements AzurePrivateDnsZoneDescriptor {

    AKS("Microsoft.ContainerService/managedClusters", "managedClusters",
            "privatelink.{region}.azmk8s.io or {subzone}.privatelink.{region}.azmk8s.io",
            List.of(DnsZoneNamePatternConstants.AKS_DNS_ZONE_NAME_PATTERN_REGION,
                    DnsZoneNamePatternConstants.AKS_DNS_ZONE_NAME_PATTERN_SUBZONE_AND_REGION));

    private final String resourceType;

    private final String subResource;

    private final String dnsZoneName;

    private final List<Pattern> dnsZoneNameRegexPatterns;

    AzurePrivateDnsZoneRegistrationEnum(String resourceType, String subResource, String dnsZoneName, List<String> dnsZoneNameTemplate) {
        this.resourceType = resourceType;
        this.subResource = subResource;
        this.dnsZoneName = dnsZoneName;
        this.dnsZoneNameRegexPatterns = dnsZoneNameTemplate.stream().map(Pattern::compile).collect(Collectors.toList());
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getSubResource() {
        return subResource;
    }

    @Override
    public String getDnsZoneName() {
        return dnsZoneName;
    }

    @Override
    public List<Pattern> getDnsZoneNamePatterns() {
        return dnsZoneNameRegexPatterns;
    }

    @Override
    public String toString() {
        return "AzurePrivateDnsZoneRegistrationEnum{" +
                "resourceType='" + resourceType + '\'' +
                ", subResource='" + subResource + '\'' +
                ", dnsZoneName='" + dnsZoneName + '\'' +
                ", dnsZoneNameRegexPatterns=" + dnsZoneNameRegexPatterns +
                "} " + super.toString();
    }

    private static class DnsZoneNamePatternConstants {
        private static final String AKS_DNS_ZONE_NAME_PATTERN_REGION = "privatelink\\.[a-z\\d-]+.azmk8s.io";

        private static final String AKS_DNS_ZONE_NAME_PATTERN_SUBZONE_AND_REGION = "[\\w+-]+\\.privatelink\\.[a-z\\d-]+.azmk8s.io";

        private DnsZoneNamePatternConstants() {
        }
    }

}