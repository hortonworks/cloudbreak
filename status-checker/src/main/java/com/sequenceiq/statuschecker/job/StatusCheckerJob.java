package com.sequenceiq.statuschecker.job;

import org.springframework.scheduling.quartz.QuartzJobBean;

public abstract class StatusCheckerJob extends QuartzJobBean {

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
}
