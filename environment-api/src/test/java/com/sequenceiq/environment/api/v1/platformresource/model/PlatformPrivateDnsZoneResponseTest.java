package com.sequenceiq.environment.api.v1.platformresource.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class PlatformPrivateDnsZoneResponseTest {

    @Test
    public void testReturnsFormattedDisplayNameWhenIdMatchesPattern() {
        PlatformPrivateDnsZoneResponse response = new PlatformPrivateDnsZoneResponse(
                "/subscriptions/123/resourceGroups/myResourceGroup/providers/Microsoft.Network/privateDnsZones/myDnsZone");
        String displayName = response.getDisplayName();
        assertEquals("myResourceGroup - myDnsZone", displayName);
    }

    @Test
    public void testReturnsIdAsDisplayNameWhenIdDoesNotMatchPattern() {
        PlatformPrivateDnsZoneResponse response = new PlatformPrivateDnsZoneResponse("invalidId");
        String displayName = response.getDisplayName();
        assertEquals("invalidId", displayName);
    }

    @Test
    public void testReturnsCachedDisplayNameOnSubsequentCalls() {
        PlatformPrivateDnsZoneResponse response = new PlatformPrivateDnsZoneResponse(
                "/subscriptions/123/resourceGroups/myResourceGroup/providers/Microsoft.Network/privateDnsZones/myDnsZone");
        String firstCall = response.getDisplayName();
        String secondCall = response.getDisplayName();
        assertSame(firstCall, secondCall);
    }

    @Test
    public void testHandlesEmptyStringId() {
        PlatformPrivateDnsZoneResponse response = new PlatformPrivateDnsZoneResponse("");
        String displayName = response.getDisplayName();
        assertEquals("", displayName);
    }

    @Test
    public void testHandlesIdWithUnexpectedFormat() {
        PlatformPrivateDnsZoneResponse response = new PlatformPrivateDnsZoneResponse("/unexpected/format/of/id");
        String displayName = response.getDisplayName();
        assertEquals("/unexpected/format/of/id", displayName);
    }

}