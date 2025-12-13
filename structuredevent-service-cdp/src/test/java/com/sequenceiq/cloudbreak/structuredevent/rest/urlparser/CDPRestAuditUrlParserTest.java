package com.sequenceiq.cloudbreak.structuredevent.rest.urlparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CDPRestAuditUrlParserTest {

    public static final String SERVICE_CONTEXT_PATH = "/aservice";

    private CDPRestAuditUrlParser underTest;

    @BeforeEach
    void setUp() {
        underTest = new CDPRestAuditUrlParser(SERVICE_CONTEXT_PATH);
    }

    @ParameterizedTest(name = "test path: {0}, it should match: {1}, name: {2}, crn: {3}")
    @MethodSource("requestPathProvider")
    void testPatternExtractionParameterized(String requestedPath, boolean match, String expectedName, String expectedCrn, String expectedEvent) {
        Pattern pattern = underTest.getPattern();
        Matcher matcher = pattern.matcher(requestedPath);
        assertEquals(match, matcher.matches());
        if (matcher.matches()) {
            assertEquals(expectedName, underTest.getResourceName(matcher), "the name should match");
            assertEquals(expectedCrn, underTest.getResourceCrn(matcher), "the crn should match");
            assertEquals(expectedEvent, underTest.getResourceEvent(matcher), "the event should match");
        }
    }

    public static Stream<Arguments> requestPathProvider() {
        return Stream.of(
                of(SERVICE_CONTEXT_PATH + "/api/info", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/health/readiness", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/health/liveness", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v1/distrox/crn/test:crn:bla/extrastuff/andmore", true, null, "test:crn:bla", "extrastuff/andmore"),
                of(SERVICE_CONTEXT_PATH + "/api/v1/distrox/newdomain/crn/test:crn:blab/extrastuff/andmore/", true, null, "test:crn:blab",
                        "extrastuff/andmore/"),
                of(SERVICE_CONTEXT_PATH + "/api/v2/distrox", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v2/name/almafa/", true, "almafa", null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v2/crn/crn%3Acdp%3Adatalake/", true, null, "crn%3Acdp%3Adatalake", null),
                of(SERVICE_CONTEXT_PATH + "/api/v2/crn/crn%3Acdp%3Adatalake", true, null, "crn%3Acdp%3Adatalake", null),
                of(SERVICE_CONTEXT_PATH + "/api/v2/distrox/", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v1/distrox/name/asdf/event", true, "asdf", null, "event"),
                of(SERVICE_CONTEXT_PATH + "/api/flow/check/chainId/354038d6-4776-4cb7-867f-fbb348b1e415", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/41/stacks/dl-wclccbzpia", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v1/env/crnByName/psqlmon/", true, "psqlmon", null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v1/env/crnByName/psqlmon", true, "psqlmon", null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/events/crn/crn:cdp:datalake:us-west-1/list", true, null, "crn:cdp:datalake:us-west-1", "list"),
                of(SERVICE_CONTEXT_PATH + "/api/v4/2/recipes/name/dex-amuk7t-cloud-storage-cleaner-sh-pre-termination", true,
                        "dex-amuk7t-cloud-storage-cleaner-sh-pre-termination", null, null),

                of(SERVICE_CONTEXT_PATH + "/api/v4/2/stacks/vedakadam1-mow-priv-aws-dl", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/12/stacks/alim4-mow-account-helper-aws-dl", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/41/stacks/internal/crn:cdp:datalake:us-west-1:1cad-45d7:datalake:20a6-4b2f/rotate_salt_password/status",
                        false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/utils/deployment", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/user_profiles", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/1/blueprints_util/scalerecommendation_by_datahub_crn", false, null, null, null),
                of(SERVICE_CONTEXT_PATH + "/api/v4/dbconfig/connectionparams", false, null, null, null)
        );
    }
}