package com.sequenceiq.cloudbreak.orchestrator.salt.domain

import com.google.common.collect.Multimap

enum class JobState {
    NOT_STARTED, IN_PROGRESS, FAILED, FINISHED;

    var nodesWithError: Multimap<String, String>? = null

}
