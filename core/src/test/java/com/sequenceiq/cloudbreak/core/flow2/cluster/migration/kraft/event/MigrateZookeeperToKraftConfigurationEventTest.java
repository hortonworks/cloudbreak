package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MigrateZookeeperToKraftConfigurationEventTest {

    @Test
    @DisplayName("kraftInstallNeeded should be final and private")
    void kraftInstallNeededFieldIsFinal() throws Exception {
        Field field = MigrateZookeeperToKraftConfigurationEvent.class.getDeclaredField("kraftInstallNeeded");
        assertTrue(Modifier.isFinal(field.getModifiers()),
                () -> "Expected 'kraftInstallNeeded' to be final, but modifiers were: " + Modifier.toString(field.getModifiers())
        );
        assertTrue(Modifier.isPrivate(field.getModifiers()),
                () -> "Expected 'kraftInstallNeeded' to be private, but modifiers were: " + Modifier.toString(field.getModifiers())
        );
    }

}