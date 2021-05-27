package com.sequenceiq.cloudbreak.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.diagnostics.DiagnosticsCollectionStatus;

public class FlowPayloadToDiagnosticDetailsConverterTest {

    private static final String SAMPLE_INPUT = "/samples/diagnostics_payload.json";

    private static final int DUMMY_PERCENT = 1;

    private static final int MAX_PERCENT = 100;

    private FlowPayloadToDiagnosticDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new FlowPayloadToDiagnosticDetailsConverter();
    }

    @Test
    public void testConvert() throws IOException {
        // GIVEN
        String sampleContent;
        try (InputStream responseStream = FlowPayloadToDiagnosticDetailsConverterTest.class.getResourceAsStream(SAMPLE_INPUT)) {
            sampleContent = IOUtils.toString(responseStream);
        }
        // WHEN
        Map<String, Object> result = underTest.convert(sampleContent);
        // THEN
        assertEquals("CLOUD_STORAGE", result.get("destination"));
        assertEquals("cloudera", result.get("accountId"));
        assertEquals("s3://cb-group/oszabo/logs/cluster-logs/freeipa/my/diagnostics", result.get("output"));
        assertEquals("FREEIPA", result.get("clusterType"));
        assertEquals("crn:cdp:freeipa:us-west-1:cloudera:freeipa:4428e540-a878-42b1-a1d4-91747322d8b6", result.get("resourceCrn"));
        assertEquals("mycase", result.get("case"));
        assertEquals("eecb0472-e39b-4383-b383-d487d60955df", result.get("uuid"));
        assertNull(result.get("description"));
    }

    @Test
    public void testCalculateProgressPercentage() {
        // GIVEN
        // WHEN
        int result = underTest.calculateProgressPercentage(false, false, () -> DUMMY_PERCENT);
        // THEN
        assertEquals(DUMMY_PERCENT, result);
    }

    @Test
    public void testCalculateProgressPercentageFinalized() {
        // GIVEN
        // WHEN
        int result = underTest.calculateProgressPercentage(true, false, () -> DUMMY_PERCENT);
        // THEN
        assertEquals(MAX_PERCENT, result);
    }

    @Test
    public void testCalculateStatusServiceStatus() {
        // GIVEN
        // WHEN
        DiagnosticsCollectionStatus status = underTest.calculateStatus("currentState", "finished", "failed",
                "failedEvent", "nextEvent", false, false);
        // THEN
        assertEquals(DiagnosticsCollectionStatus.IN_PROGRESS, status);
    }

    @Test
    public void testCalculateStatusServiceStatusFailed() {
        // GIVEN
        // WHEN
        DiagnosticsCollectionStatus status = underTest.calculateStatus("currentState", "finished", "failed",
                "failedEvent", "nextEvent", false, true);
        // THEN
        assertEquals(DiagnosticsCollectionStatus.FAILED, status);
    }

    @Test
    public void testCalculateStatusServiceFinished() {
        // GIVEN
        // WHEN
        DiagnosticsCollectionStatus status = underTest.calculateStatus("finished", "finished", "failed",
                "failedEvent", "nextEvent", true, false);
        // THEN
        assertEquals(DiagnosticsCollectionStatus.FINISHED, status);
    }

    @Test
    public void testCalculateStatusServiceFinishedAndFailed() {
        // GIVEN
        // WHEN
        DiagnosticsCollectionStatus status = underTest.calculateStatus("finished", "finished", "failed",
                "failedEvent", "nextEvent", true, true);
        // THEN
        assertEquals(DiagnosticsCollectionStatus.FAILED, status);
    }

    @Test
    public void testCalculateStatusServiceFinishedAndFailedNextEvent() {
        // GIVEN
        // WHEN
        DiagnosticsCollectionStatus status = underTest.calculateStatus("finished", "finished", "failed",
                "failedEvent", "failedEvent", true, true);
        // THEN
        assertEquals(DiagnosticsCollectionStatus.FAILED, status);
    }
}
