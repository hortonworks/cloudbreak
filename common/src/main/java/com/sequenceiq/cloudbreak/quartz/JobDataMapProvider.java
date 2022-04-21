package com.sequenceiq.cloudbreak.quartz;

import org.quartz.JobDataMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;

@Component
public class JobDataMapProvider {

    public static final String APP_VERSION_KEY = "appVersion";

    private final String appVersion;

    public JobDataMapProvider(@Value("${info.app.version:}") String appVersion) {
        this.appVersion = appVersion;
    }

    public JobDataMap provide(JobResourceAdapter<?> jobResourceAdapter) {
        return addAppVersionToJobDataMap(jobResourceAdapter.toJobDataMap());
    }

    /**
     * Add app version to jobDataMap. It will ensure that jobs will not be executed by older app versions.
     *
     * @param jobDataMap input
     * @return jobDataMap extended with app version
     */
    public JobDataMap addAppVersionToJobDataMap(JobDataMap jobDataMap) {
        jobDataMap.put(APP_VERSION_KEY, appVersion);
        return jobDataMap;
    }

}
