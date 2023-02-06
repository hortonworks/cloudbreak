package com.sequenceiq.cloudbreak.quartz.metric;

import org.quartz.JobDataMap;

import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;

public class QuartzMetricUtil {

    private static final String PROVIDER_NONE = "NONE";

    private QuartzMetricUtil() {
    }

    public static String getProvider(JobDataMap jobDataMap) {
        if (jobDataMap.containsKey(JobResourceAdapter.PROVIDER)) {
            return jobDataMap.getString(JobResourceAdapter.PROVIDER);
        } else {
            return PROVIDER_NONE;
        }
    }
}
