package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.ENVIRONMENT_RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class EnvironmentUrlParserTest {

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameter(1)
    public boolean match;

    @Parameterized.Parameter(2)
    public String resourceCrn;

    @Parameterized.Parameter(3)
    public String resourceName;

    @Parameterized.Parameter(4)
    public String resourceType;

    @Parameterized.Parameter(5)
    public String resourceEvent;

    private EnvironmentUrlParser underTest;

    @Before
    public void init() {
        underTest = new EnvironmentUrlParser();
    }

    @Test
    public void testMatch() {
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

    @Parameterized.Parameters(name
            = "{index}: testMatch(url: {0}, should match {1}, resourceCrn: {2}, resourceName: {3}, resourceType: {4}, resourceEvent: {5}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"v1/env/name/asdf", true, null, "asdf", ENVIRONMENT_RESOURCE_TYPE, "asdf"},
                {"v1/env/crnByName/asdf", true, null, "asdf", ENVIRONMENT_RESOURCE_TYPE, "asdf"},
                {"v1/env/random/asdf", false, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v1/env/name/asdf/event", true, null, "asdf", ENVIRONMENT_RESOURCE_TYPE, "event"},
                {"v1/name/asdf", false, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"env", false, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v2/env", false, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v1/env", true, null, null, ENVIRONMENT_RESOURCE_TYPE, null},
                {"v1/env/name/event", true, null, "event", ENVIRONMENT_RESOURCE_TYPE, "event"},
                {"v1/env/crn/event", true, "event", null, ENVIRONMENT_RESOURCE_TYPE, "event"},
                {"v1/env/crn/test:crn:blabla", true, "test:crn:blabla", null, ENVIRONMENT_RESOURCE_TYPE, "test:crn:blabla"},
                {"v1/env/crn/test:crn:blabla/extrastuff", true, "test:crn:blabla", null, ENVIRONMENT_RESOURCE_TYPE, "extrastuff"},
                {"v1/env/crn/test:crn:blabla/extrastuff/andmore", true, "test:crn:blabla", null, ENVIRONMENT_RESOURCE_TYPE, "extrastuff/andmore"}
        });
    }
}
