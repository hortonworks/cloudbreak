package com.sequenceiq.flow;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.event.Payload;

public class EnforceEntityDenialUtil {

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq",
            new SubTypesScanner(false));

    private EnforceEntityDenialUtil() {

    }

    public static void denyEntity() {
        Set<String> eventsWithEntities = Sets.union(REFLECTIONS.getSubTypesOf(Payload.class), REFLECTIONS.getSubTypesOf(IdempotentEvent.class))
                .stream()
                .filter(payload -> Arrays.stream(payload.getDeclaredFields()).anyMatch(field -> field.getType().isAnnotationPresent(Entity.class)))
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertTrue(eventsWithEntities.isEmpty(), String.format("These flow payloads/events should not contain entity: %n%s ",
                Joiner.on("\n").join(eventsWithEntities)));
    }
}
