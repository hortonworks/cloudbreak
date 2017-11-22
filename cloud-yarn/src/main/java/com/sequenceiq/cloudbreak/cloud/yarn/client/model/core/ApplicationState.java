package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

/**
 * The current state of an application
 **/
public enum ApplicationState {
    ACCEPTED, STARTED, READY, STOPPED, FAILED
}