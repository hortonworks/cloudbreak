package com.sequenceiq.freeipa.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.KERBEROS_RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class KerberosConfigUrlParserTest {

    private final KerberosConfigUrlParser underTest = new KerberosConfigUrlParser();

    @ParameterizedTest(name
            = "{index}: testMatch(url: {0}, should match {1}, resourceType: {2}, resourceEvent: {3}")
    @MethodSource
    void testMatch(String url, boolean match, String resourceType, String resourceEvent) {
        Matcher matcher = underTest.getPattern().matcher(url);
        if (match) {
            assertTrue(matcher.matches());
            assertEquals(resourceType, underTest.getResourceType(matcher));
            assertEquals(resourceEvent, underTest.getResourceEvent(matcher));
        } else {
            assertFalse(matcher.matches());
        }
    }

    public static Iterable<Object[]> testMatch() {
        return Arrays.asList(new Object[][]{
                {"v1/kerberos", true, KERBEROS_RESOURCE_TYPE, null},
                {"v1/kerberos/", true, KERBEROS_RESOURCE_TYPE, null},
                {"v1/kerberos/event/asdf/", true, KERBEROS_RESOURCE_TYPE, "event"},
                {"v1/kerberos/event/", true, KERBEROS_RESOURCE_TYPE, "event"},
                {"v1/name/asdf", false, KERBEROS_RESOURCE_TYPE, null},
                {"kerberos", false, KERBEROS_RESOURCE_TYPE, null},
                {"v1/kerberos/name/event", true, KERBEROS_RESOURCE_TYPE, "name"},
        });
    }
}
