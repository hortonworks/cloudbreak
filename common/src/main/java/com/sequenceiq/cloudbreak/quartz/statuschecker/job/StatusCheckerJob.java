package com.sequenceiq.cloudbreak.quartz.statuschecker.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;

public abstract class StatusCheckerJob extends MdcQuartzJob {

    private String localId;

    private String remoteResourceCrn;

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getRemoteResourceCrn() {
        return remoteResourceCrn;
    }

    public void setRemoteResourceCrn(String remoteResourceCrn) {
        this.remoteResourceCrn = remoteResourceCrn;
    }

    protected Long getLocalIdAsLong() {
        return Long.valueOf(getLocalId());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        ThreadBasedUserCrnProvider.doAsInternalActor(() -> executeJob(context), Crn.safeFromString(remoteResourceCrn).getAccountId());
    }

    protected abstract void executeJob(JobExecutionContext context) throws JobExecutionException;
}
