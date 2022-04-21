package com.sequenceiq.cloudbreak.quartz.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import com.sequenceiq.cloudbreak.quartz.JobDataMapProvider;

@ExtendWith(MockitoExtension.class)
class JobAppVersionVerifierTest {

    private static final String APP_VERSION = "2.56.0-b28-2-gd167eb0";

    private final JobAppVersionVerifier underTest = new JobAppVersionVerifier(APP_VERSION);

    @Mock
    private JobExecutionContext context;

    @Mock
    private Trigger trigger;

    @BeforeEach
    void setUp() {
        lenient().when(context.getJobDetail()).thenReturn(mock(JobDetail.class));
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());
    }

    @Test
    void testMissingJobVersion() {
        boolean veto = underTest.vetoJobExecution(trigger, context);

        assertFalse(veto);
    }

    @Test
    void testOlderJobVersion() {
        setJobAppVersion("2.54.0-b42-2-gd167eb0");

        boolean veto = underTest.vetoJobExecution(trigger, context);

        assertFalse(veto);
    }

    @Test
    void testSameJobVersion() {
        setJobAppVersion(APP_VERSION);

        boolean veto = underTest.vetoJobExecution(trigger, context);

        assertFalse(veto);
    }

    @Test
    void testNewerJobVersion() {
        setJobAppVersion("2.57.0-b2-1-gd167eb0");

        boolean veto = underTest.vetoJobExecution(trigger, context);

        assertTrue(veto);
    }

    private void setJobAppVersion(String jobAppVersion) {
        context.getMergedJobDataMap().put(JobDataMapProvider.APP_VERSION_KEY, jobAppVersion);
    }

}
