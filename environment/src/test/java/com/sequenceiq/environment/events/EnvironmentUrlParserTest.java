package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.ENVIRONMENT_RESOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EnvironmentUrlParserTest {

    private final EnvironmentUrlParser underTest = new EnvironmentUrlParser();

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
                {"v1/env/name/asdf", true, "name", null, "asdf", ENVIRONMENT_RESOURCE_TYPE, "asdf"},
                {"v1/env/crnByName/asdf", true, "crnByName", null, "asdf", ENVIRONMENT_RESOURCE_TYPE, "asdf"},
                {"v1/env/random/asdf", false, "random", null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v1/env/name/asdf/event", true, "name", null, "asdf", ENVIRONMENT_RESOURCE_TYPE, "event"},
                {"v1/name/asdf", false, "name", null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"env", false, null, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v2/env", false, null, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v1/env", true, null, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v1/env/name/event", true, "name", null, "event", ENVIRONMENT_RESOURCE_TYPE, "event"},
                {"v1/env/crn/event", true, "crn", "event", null, ENVIRONMENT_RESOURCE_TYPE, "event"},
                {"v1/env/crn/test:crn:blabla", true, "crn", "test:crn:blabla", null, ENVIRONMENT_RESOURCE_TYPE, "test:crn:blabla"},
                {"v1/env/crn/test:crn:blabla/extrastuff", true, "crn", "test:crn:blabla", null, ENVIRONMENT_RESOURCE_TYPE, "extrastuff"},
                {"v1/env/crn/test:crn:blabla/extrastuff/andmore", true, "crn", "test:crn:blabla", null, ENVIRONMENT_RESOURCE_TYPE, "extrastuff/andmore"}
        });
    }
}
