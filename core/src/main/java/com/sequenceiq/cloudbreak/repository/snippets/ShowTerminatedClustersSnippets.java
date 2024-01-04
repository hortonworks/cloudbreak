package com.sequenceiq.cloudbreak.repository.snippets;

public class ShowTerminatedClustersSnippets {

    public static final String SHOW_TERMINATED_CLUSTERS_IF_REQUESTED = " (s.terminated is null OR :showTerminated = true AND s.terminated > :terminatedAfter) ";

    private ShowTerminatedClustersSnippets() {
    }
}
