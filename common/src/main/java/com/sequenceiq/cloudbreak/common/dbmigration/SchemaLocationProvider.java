package com.sequenceiq.cloudbreak.common.dbmigration;

import java.util.Optional;

public interface SchemaLocationProvider {

    Optional<String> pendingSubfolder();

    default Optional<String> upSubfolder() {
        return Optional.empty();
    }
}
