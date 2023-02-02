package com.sequenceiq.cloudbreak.quartz.model;

import java.util.Optional;

public interface JobResource {

    String getLocalId();

    String getRemoteResourceId();

    String getName();

    Optional<String> getProvider();
}
