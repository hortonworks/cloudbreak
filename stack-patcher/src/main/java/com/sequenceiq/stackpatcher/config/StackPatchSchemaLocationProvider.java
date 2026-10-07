package com.sequenceiq.stackpatcher.config;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.dbmigration.SchemaLocationProvider;

@Component
public class StackPatchSchemaLocationProvider implements SchemaLocationProvider {

    @Override
    public Optional<String> pendingSubfolder() {
        return Optional.of("stackpatch");
    }
}
