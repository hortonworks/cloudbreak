package com.sequenceiq.cloudbreak.quartz;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;

import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;

@ExtendWith(MockitoExtension.class)
class JobDataMapProviderTest {

    private static final String APP_VERSION = "2.56.0-b28-2-gd167eb0";

    private static final Map<String, String> EXISTING_JOB_PARAMS = Map.of("existing-key", "existing-value");

    private static final JobDataMap JOB_DATA_MAP = new JobDataMap(EXISTING_JOB_PARAMS);

    private final JobDataMapProvider underTest = new JobDataMapProvider(APP_VERSION);

    @Test
    void addAppVersionToExistingJobDataMap() {
        JobDataMap result = underTest.addAppVersionToJobDataMap(JOB_DATA_MAP);

        verifyResult(result);
    }

    @Test
    void provideForJoBResourceAdapter() {
        JobResourceAdapter<?> jobResourceAdapter = mock(JobResourceAdapter.class);
        when(jobResourceAdapter.toJobDataMap()).thenReturn(JOB_DATA_MAP);

        JobDataMap result = underTest.provide(jobResourceAdapter);

        verifyResult(result);
    }

    private void verifyResult(JobDataMap result) {
        Assertions.assertThat(result)
                .containsAllEntriesOf(EXISTING_JOB_PARAMS)
                .containsEntry(JobDataMapProvider.APP_VERSION_KEY, APP_VERSION);
    }

}
