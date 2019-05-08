package com.sequenceiq.cloudbreak.common.dbmigration;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class DefaultSchemaLocationProvider implements SchemaLocationProvider {

    @Override
    public Optional<String> pendingSubfolder() {
        return Optional.of("app");
    }

    @Override
    public Optional<String> upSubfolder() {
        return Optional.of("mybatis");
    }
}
