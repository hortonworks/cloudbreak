package com.sequenceiq.cloudbreak.orchestrator.model;

public class CmAgentStopFlags {

    private final boolean adJoinable;

    private final boolean ipaJoinable;

    private final boolean forced;

    public CmAgentStopFlags(boolean adJoinable, boolean ipaJoinable, boolean forced) {
        this.adJoinable = adJoinable;
        this.ipaJoinable = ipaJoinable;
        this.forced = forced;
    }

    public boolean isAdJoinable() {
        return adJoinable;
    }

    public boolean isIpaJoinable() {
        return ipaJoinable;
    }

    public boolean isForced() {
        return forced;
    }
}
