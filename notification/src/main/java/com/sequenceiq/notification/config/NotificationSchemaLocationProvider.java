package com.sequenceiq.notification.config;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.dbmigration.SchemaLocationProvider;

/**
 * Schema location provider for notification module migrations.
 * This makes notification module's migration scripts available to services that depend on it.
 */
@Component
public class NotificationSchemaLocationProvider implements SchemaLocationProvider {

    @Override
    public Optional<String> pendingSubfolder() {
        return Optional.of("notification");
    }
}

