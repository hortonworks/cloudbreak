package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;

@Service
public class AzurePrivateDnsZoneMatcherService {

    public boolean isZoneNameMatchingPattern(AzurePrivateDnsZoneDescriptor privateDnsZoneDescriptor, String privateDnsZoneName) {
        return privateDnsZoneDescriptor.getDnsZoneNamePatterns().stream().anyMatch(namePattern -> matches(privateDnsZoneName, namePattern));
    }

    private static boolean matches(String privateDnsZoneName, Pattern namePattern) {
        Matcher matcher = namePattern.matcher(privateDnsZoneName);
        return matcher.matches();
    }

}