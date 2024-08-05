package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformPrivateDnsZoneResponse implements Serializable {

    private static final Pattern DNS_ZONE_DISPLAY_NAME_PATTERN = Pattern.compile(
            "/subscriptions/[^/]+/resourceGroups/([^/]+)/providers/Microsoft\\.Network/privateDnsZones/([^/]+)");

    private String id;

    private String displayName;

    public PlatformPrivateDnsZoneResponse() {
    }

    public PlatformPrivateDnsZoneResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        if (displayName == null) {
            Matcher matcher = DNS_ZONE_DISPLAY_NAME_PATTERN.matcher(id);
            if (matcher.find()) {
                String resourceGroupName = matcher.group(1);
                String dnsZoneName = matcher.group(2);
                displayName = String.format("%s - %s", resourceGroupName, dnsZoneName);
            } else {
                displayName = id;
            }
        }
        return displayName;
    }

    @Override
    public String toString() {
        return "PlatformPrivateDnsZoneResponse{" +
                "id='" + id + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                '}';
    }

}