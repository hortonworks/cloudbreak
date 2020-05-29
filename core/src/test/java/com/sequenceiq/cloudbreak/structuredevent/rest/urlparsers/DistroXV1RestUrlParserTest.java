package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.DATAHUB_RESOURCE_TYPE;
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
public class DistroXV1RestUrlParserTest {

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

    private DistroXV1RestUrlParser underTest;

    @Before
    public void init() {
        underTest = new DistroXV1RestUrlParser();
    }

    @Test
    public void testMatch() {
        Matcher matcher = underTest.getPattern().matcher(url);
        if (match) {
            assertTrue(matcher.matches());
            assertEquals(resourceName, underTest.getResourceName(matcher));
            assertEquals(resourceCrn, underTest.getResourceCrn(matcher));
            assertEquals(resourceType, underTest.getResourceType(matcher));
        } else {
            assertFalse(matcher.matches());
        }
    }

    @Parameterized.Parameters(name
            = "{index}: testMatch(url: {0}, should match {1}, resourceCrn: {2}, resourceName: {3}, resourceType: {4}, resourceEvent: {5}")
    public static Iterable<Object[]> data() {
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