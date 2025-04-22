package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsFlowServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environment:eu-1:1234:user:91011";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:1234:user:91011";

    @InjectMocks
    private DiagnosticsFlowService underTest;

    @Mock
    private Stack stack;

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private CdpDoctorService cdpDoctorService;

    @Test
    public void testIsVersionGreaterOrEqualIfVersionEquals() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.8", "0.4.8");
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionLess() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.7", "0.4.8");
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionGreater() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.9", "0.4.8");
        // THEN
        assertTrue(result);
    }
}
