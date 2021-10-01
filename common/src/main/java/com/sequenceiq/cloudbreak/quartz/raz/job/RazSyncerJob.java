package com.sequenceiq.cloudbreak.quartz.raz.job;

import com.sequenceiq.cloudbreak.quartz.TracedQuartzJob;

import io.opentracing.Tracer;

public abstract class RazSyncerJob extends TracedQuartzJob {

    private String localId;

    private String remoteResourceCrn;

    public RazSyncerJob(Tracer tracer, String jobName) {
        super(tracer, jobName);
    }

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
}
