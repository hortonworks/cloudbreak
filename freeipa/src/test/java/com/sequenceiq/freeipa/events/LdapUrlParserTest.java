package com.sequenceiq.freeipa.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.LDAP_RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LdapUrlParserTest {

    private final LdapUrlParser underTest = new LdapUrlParser();

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
                {"v1/ldaps", true, LDAP_RESOURCE_TYPE, null},
                {"v1/ldaps/", true, LDAP_RESOURCE_TYPE, null},
                {"v1/ldaps/event/asdf/", true, LDAP_RESOURCE_TYPE, "event"},
                {"v1/ldaps/event/", true, LDAP_RESOURCE_TYPE, "event"},
                {"v1/name/asdf", false, LDAP_RESOURCE_TYPE, null},
                {"ldaps", false, LDAP_RESOURCE_TYPE, null},
                {"v1/ldaps/name/event", true, LDAP_RESOURCE_TYPE, "name"},
        });
    }
}
