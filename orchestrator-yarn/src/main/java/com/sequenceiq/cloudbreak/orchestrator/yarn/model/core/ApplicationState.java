package com.sequenceiq.cloudbreak.orchestrator.yarn.model.core;

/**
 * The current state of an application
 **/
public enum ApplicationState {
    ACCEPTED, STARTED, READY, STOPPED, FAILED
}