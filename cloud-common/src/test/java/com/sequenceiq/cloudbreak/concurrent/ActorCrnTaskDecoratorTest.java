package com.sequenceiq.cloudbreak.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

class ActorCrnTaskDecoratorTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ACTOR_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private ActorCrnTaskDecorator underTest = new ActorCrnTaskDecorator();

    @BeforeEach
    void setUp() {
        assertNull(ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Test
    void decorateWithActorCrn() {
        ThreadBasedUserCrnProvider.doAs(ACTOR_CRN, () -> assertRunnableWithActorCrn(ACTOR_CRN));
    }

    @Test
    void decorateWithNoActorCrn() {
        assertRunnableWithActorCrn(null);
    }

    private void assertRunnableWithActorCrn(String expectedUserCrn) {
        underTest.decorate(() -> {
            assertEquals(expectedUserCrn, ThreadBasedUserCrnProvider.getUserCrn());
        }).run();
    }
}