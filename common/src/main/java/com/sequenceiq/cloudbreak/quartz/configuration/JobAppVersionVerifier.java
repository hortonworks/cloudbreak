package com.sequenceiq.cloudbreak.quartz.configuration;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.JobDataMapProvider;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class JobAppVersionVerifier extends TriggerListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobAppVersionVerifier.class);

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    private final String appVersion;

    public JobAppVersionVerifier(@Value("${info.app.version:}") String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Veto job execution if the job was scheduled with a newer version app than the current app version.
     *
     * @param trigger The <code>Trigger</code> that has fired.
     * @param context The <code>JobExecutionContext</code> that will be passed to the <code>Job</code>'s<code>execute(xx)</code> method.
     * @return whether veto the job execution
     */
    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        String jobAppVersion = context.getMergedJobDataMap().getString(JobDataMapProvider.APP_VERSION_KEY);
        boolean jobIsNewerThanApp = jobAppVersion != null && VERSION_COMPARATOR.compare(() -> appVersion, () -> jobAppVersion) < 0;
        if (jobIsNewerThanApp) {
            LOGGER.info("Vetoing job {} execution, because its version ({}) is newer than app version ({})",
                    context.getJobDetail().getKey(), jobAppVersion, appVersion);
        }
        return jobIsNewerThanApp;
    }

    @Override
    public String getName() {
        return JobAppVersionVerifier.class.getName();
    }
}
