package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.CREDENTIAL_RESOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CredentialUrlParserTest {

    private final CredentialUrlParser underTest = new CredentialUrlParser();

    @ParameterizedTest(name
            = "{index}: testMatch(url: {0}, should match {1}, idType: {2}, resourceCrn: {3}, resourceName: {4}, resourceType: {5}, resourceEvent: {6}")
    @MethodSource
    public void testMatch(String url, boolean match, String idType, String resourceCrn, String resourceName, String resourceType, String resourceEvent) {
        Matcher matcher = underTest.getPattern().matcher(url);
        if (match) {
            assertTrue(matcher.matches());
            assertEquals(idType, underTest.getIdType(matcher));
            assertEquals(resourceName, underTest.getResourceName(matcher));
            assertEquals(resourceCrn, underTest.getResourceCrn(matcher));
            assertEquals(resourceType, underTest.getResourceType(matcher));
            assertEquals(resourceEvent, underTest.getResourceEvent(matcher));
        } else {
            assertFalse(matcher.matches());
        }
    }

    public static Iterable<Object[]> testMatch() {
        return Arrays.asList(new Object[][]{
                {"v1/credentials/name/asdf", true, "name", null, "asdf", CREDENTIAL_RESOURCE_TYPE, "asdf"},
                {"v1/credentials/crnByName/asdf", true, "crnByName", null, "asdf", CREDENTIAL_RESOURCE_TYPE, "asdf"},
                {"v1/credentials/random/asdf", false, "random", null, null, CREDENTIAL_RESOURCE_TYPE, null},
                {"v1/credentials/name/asdf/event", true, "name", null, "asdf", CREDENTIAL_RESOURCE_TYPE, "event"},
                {"v1/name/asdf", false, "name", null, null, CREDENTIAL_RESOURCE_TYPE, null},
                {"credentials", false, null, null, null, CREDENTIAL_RESOURCE_TYPE, null},
                {"v2/credentials", false, null, null, null, CREDENTIAL_RESOURCE_TYPE, null},
                {"v1/credentials", true, null, null, null, CREDENTIAL_RESOURCE_TYPE, null},
                {"v1/credentials/name/event", true, "name", null, "event", CREDENTIAL_RESOURCE_TYPE, "event"},
                {"v1/credentials/crn/event", true, "crn", "event", null, CREDENTIAL_RESOURCE_TYPE, "event"},
                {"v1/credentials/crn/test:crn:blabla", true, "crn", "test:crn:blabla", null, CREDENTIAL_RESOURCE_TYPE, "test:crn:blabla"},
                {"v1/credentials/crn/test:crn:blabla/extrastuff", true, "crn", "test:crn:blabla", null, CREDENTIAL_RESOURCE_TYPE, "extrastuff"},
                {"v1/credentials/crn/test:crn:blabla/extrastuff/andmore", true, "crn", "test:crn:blabla", null, CREDENTIAL_RESOURCE_TYPE, "extrastuff/andmore"}
        });
    }
}
