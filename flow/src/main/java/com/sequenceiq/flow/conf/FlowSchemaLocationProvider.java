package com.sequenceiq.flow.conf;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.dbmigration.SchemaLocationProvider;

@Component
public class FlowSchemaLocationProvider implements SchemaLocationProvider {

    @Override
    public Optional<String> pendingSubfolder() {
        return Optional.of("flow");
    }
}
