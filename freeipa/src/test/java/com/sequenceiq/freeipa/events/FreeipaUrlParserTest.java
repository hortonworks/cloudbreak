package com.sequenceiq.freeipa.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.FREEIPA_RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FreeipaUrlParserTest {

    private final FreeipaUrlParser underTest = new FreeipaUrlParser();

    @ParameterizedTest(name
            = "{index}: testMatch(url: {0}, should match {1}, resourceType: {2}, resourceEvent: {3}")
    @MethodSource
    public void testMatch(String url, boolean match, String resourceType, String resourceEvent) {
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
                {"v1/freeipa", true, FREEIPA_RESOURCE_TYPE, null},
                {"v1/freeipa/", true, FREEIPA_RESOURCE_TYPE, null},
                {"v1/freeipa/event/asdf/", true, FREEIPA_RESOURCE_TYPE, "event"},
                {"v1/freeipa/event/", true, FREEIPA_RESOURCE_TYPE, "event"},
                {"v1/name/asdf", false, FREEIPA_RESOURCE_TYPE, null},
                {"freeipa", false, FREEIPA_RESOURCE_TYPE, null},
                {"v1/freeipa/name/event", true, FREEIPA_RESOURCE_TYPE, "name"},
        });
    }
}
