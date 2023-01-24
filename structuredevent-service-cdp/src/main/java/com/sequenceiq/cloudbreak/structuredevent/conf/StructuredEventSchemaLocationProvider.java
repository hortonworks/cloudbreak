package com.sequenceiq.cloudbreak.structuredevent.conf;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.dbmigration.SchemaLocationProvider;

@Component
public class StructuredEventSchemaLocationProvider implements SchemaLocationProvider {

    @Override
    public Optional<String> pendingSubfolder() {
        return Optional.of("structuredevent");
    }
}
