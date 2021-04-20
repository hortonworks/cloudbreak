package com.sequenceiq.authorization.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class HasRightOnAllTest {

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.EDIT_CREDENTIAL;

    private Function<String, Optional<String>> nameMapper = Optional::ofNullable;

    @Test
    public void testHasRightOnAll() {
        AuthorizationRule authorization = new HasRightOnAll(ACTION, List.of("crn1", "crn2"));

        List<Boolean> results = new ArrayList<>();
        authorization.convert((action, crn) -> results.add(true));

        assertEquals(2, results.size());

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.empty(), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHasRightOnAllButOne() {
        AuthorizationRule authorization = new HasRightOnAll(ACTION, List.of("crn1", "crn2", "crn3"));

        List<Boolean> results = List.of(true, false, true);

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(new HasRight(ACTION, "crn2")), failedAuthorizations);
        assertEquals("Doesn't have 'environments/editCredential' right on unknown resource type [name: crn2, crn: crn2].",
                failedAuthorizations.get().getAsFailureMessage(AuthorizationResourceAction::getRight, nameMapper));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHasRightOnAllButTwo() {
        AuthorizationRule authorization = new HasRightOnAll(ACTION, List.of("crn1", "crn2", "crn3", "crn4"));

        List<Boolean> results = List.of(true, false, true, false);

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(new HasRightOnAll(ACTION, List.of("crn2", "crn4"))), failedAuthorizations);
        assertEquals("Doesn't have 'environments/editCredential' right on unknown resource type(s) " +
                        "[name: crn2, crn: crn2] [name: crn4, crn: crn4].",
                failedAuthorizations.get().getAsFailureMessage(AuthorizationResourceAction::getRight, nameMapper));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHasRightOnNone() {
        AuthorizationRule authorization = new HasRightOnAll(ACTION, List.of("crn1", "crn2"));

        List<Boolean> results = new ArrayList<>();
        authorization.convert((action, crn) -> results.add(false));

        assertEquals(2, results.size());

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(authorization), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }
}