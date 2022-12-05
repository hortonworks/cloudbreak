package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;

public class AzurePrivateDnsZoneMatcherServiceTest {

    private static final String ZONE_NAME = "zoneName";

    private final AzurePrivateDnsZoneMatcherService underTest = new AzurePrivateDnsZoneMatcherService();

    @Test
    void testIsZoneNameMatchingPatternWhenMatchesThenReturnsTrue() {
        Pattern pattern = mock(Pattern.class);
        Matcher matcher = mock(Matcher.class);
        when(pattern.matcher(ZONE_NAME)).thenReturn(matcher);
        AzurePrivateDnsZoneDescriptor descriptor = mock(AzurePrivateDnsZoneDescriptor.class);
        when(descriptor.getDnsZoneNamePatterns()).thenReturn(List.of(pattern));
        when(matcher.matches()).thenReturn(true);

        boolean matchingResult = underTest.zoneNameMatchesPattern(descriptor, ZONE_NAME);

        assertTrue(matchingResult);
        verify(pattern).matcher(ZONE_NAME);
        verify(matcher).matches();
    }

    @Test
    void testIsZoneNameMatchingPatternWhenNoMatchThenReturnsFalse() {
        Pattern pattern = mock(Pattern.class);
        Matcher matcher = mock(Matcher.class);
        when(pattern.matcher(ZONE_NAME)).thenReturn(matcher);
        AzurePrivateDnsZoneDescriptor descriptor = mock(AzurePrivateDnsZoneDescriptor.class);
        when(descriptor.getDnsZoneNamePatterns()).thenReturn(List.of(pattern));
        when(matcher.matches()).thenReturn(false);

        boolean matchingResult = underTest.zoneNameMatchesPattern(descriptor, ZONE_NAME);

        assertFalse(matchingResult);
        verify(pattern).matcher(ZONE_NAME);
        verify(matcher).matches();
    }

}