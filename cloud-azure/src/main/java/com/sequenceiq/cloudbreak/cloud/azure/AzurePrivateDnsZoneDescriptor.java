package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.regex.Pattern;

public interface AzurePrivateDnsZoneDescriptor {

    String getResourceType();

    String getSubResource();

    String getDnsZoneName(String resourceGroupName);

    List<Pattern> getDnsZoneNamePatterns();

}