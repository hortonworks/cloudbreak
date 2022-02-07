package com.sequenceiq.cloudbreak.quartz.model;

public interface JobResource {

    String getLocalId();

    String getRemoteResourceId();

    String getName();
}
