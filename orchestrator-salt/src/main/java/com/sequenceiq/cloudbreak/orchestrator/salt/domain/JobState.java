package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import com.google.common.collect.Multimap;

public enum JobState {
    NOT_STARTED, IN_PROGRESS, FAILED, FINISHED;

    private Multimap<String, String> nodesWithError;

    public Multimap<String, String> getNodesWithError() {
        return nodesWithError;
    }

    public void setNodesWithError(Multimap<String, String> nodesWithError) {
        this.nodesWithError = nodesWithError;
    }

}
