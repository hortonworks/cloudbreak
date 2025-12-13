package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.DATAHUB_RESOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DistroXV1RestUrlParserTest {

    private DistroXV1RestUrlParser underTest = new DistroXV1RestUrlParser();

    @MethodSource("data")
    @ParameterizedTest
    void testMatch(String url, boolean match, String resourceCrn, String resourceName, String resourceType, String resourceEvent) {
        Matcher matcher = underTest.getPattern().matcher(url);
        if (match) {
            assertTrue(matcher.matches());
            assertEquals(resourceName, underTest.getResourceName(matcher));
            assertEquals(resourceCrn, underTest.getResourceCrn(matcher));
            assertEquals(resourceType, underTest.getResourceType(matcher));
            assertEquals(resourceEvent, underTest.getResourceEvent(matcher));
        } else {
            assertFalse(matcher.matches());
        }
    }

    static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"v1/distrox/name/asdf", true, null, "asdf", DATAHUB_RESOURCE_TYPE, "asdf"},
                {"v1/distrox/random/asdf", false, null, null, DATAHUB_RESOURCE_TYPE, null},
                {"v1/distrox/name/asdf/event", true, null, "asdf", DATAHUB_RESOURCE_TYPE, "event"},
                {"v1/name/asdf", false, null, null, DATAHUB_RESOURCE_TYPE, null},
                {"distrox", false, null, null, DATAHUB_RESOURCE_TYPE, null},
                {"v2/distrox", false, null, null, DATAHUB_RESOURCE_TYPE, null},
                {"v1/distrox", true, null, null, DATAHUB_RESOURCE_TYPE, null},
                {"v1/distrox/name/event", true, null, "event", DATAHUB_RESOURCE_TYPE, "event"},
                {"v1/distrox/crn/event", true, "event", null, DATAHUB_RESOURCE_TYPE, "event"},
                {"v1/distrox/crn/test:crn:blabla", true, "test:crn:blabla", null, DATAHUB_RESOURCE_TYPE, "test:crn:blabla"},
                {"v1/distrox/crn/test:crn:blabla/extrastuff", true, "test:crn:blabla", null, DATAHUB_RESOURCE_TYPE, "extrastuff"},
                {"v1/distrox/crn/test:crn:blabla/extrastuff/andmore", true, "test:crn:blabla", null, DATAHUB_RESOURCE_TYPE, "extrastuff/andmore"}
        });
    }
}