package com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

class RestAuditEventSourceExtractorTest {

    public static Stream<Arguments> applicationNameProvider() {
        return Stream.of(
                Arguments.of("EnvironmentService", false),
                Arguments.of("EnvironmentsService", true),
                Arguments.of("DatalakeService", true),
                Arguments.of("RedbeamsService", true),
                Arguments.of("FreeIpaService", true),
                Arguments.of("FreeipaService", true),
                Arguments.of("CloudbreakService", true),
                Arguments.of("DatahubService", true),
                Arguments.of("Periscope", false),
                Arguments.of("TheNewestService", false)
        );
    }

    @ParameterizedTest(name = "test with spring application name: {0} and it is valid input: {1}")
    @MethodSource("applicationNameProvider")
    void eventSource(String applicationName, boolean validInput) {
        if (validInput) {
            RestAuditEventSourceExtractor underTest = new RestAuditEventSourceExtractor(applicationName);
            Crn.Service service = underTest.eventSource();
            assertTrue(StringUtils.containsIgnoreCase(applicationName, service.getName()));
        } else {
            assertThrows(IllegalStateException.class, () -> new RestAuditEventSourceExtractor(applicationName));
        }
    }
}