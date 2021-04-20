package com.sequenceiq.authorization.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class HasRightOnAnyTest {

    @Test
    public void testHasRightOnAll() {
        AuthorizationRule authorization = new HasRightOnAny(AuthorizationResourceAction.EDIT_CREDENTIAL, List.of("crn1", "crn2"));

        List<Boolean> results = new ArrayList<>();
        authorization.convert((action, crn) -> results.add(true));

        assertEquals(2, results.size());

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.empty(), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHasRightOnOne() {
        AuthorizationRule authorization = new HasRightOnAny(AuthorizationResourceAction.EDIT_CREDENTIAL, List.of("crn1", "crn2", "crn3"));

        List<Boolean> results = List.of(false, true, false);

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.empty(), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHasRightOnNone() {
        AuthorizationRule authorization = new HasRightOnAny(AuthorizationResourceAction.EDIT_CREDENTIAL, List.of("crn1", "crn2"));

        List<Boolean> results = new ArrayList<>();
        authorization.convert((action, crn) -> results.add(false));

        assertEquals(2, results.size());

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(authorization), failedAuthorizations);
        assertEquals("Doesn't have 'environments/editCredential' right on any of the unknown resource type(s) " +
                        "[name: crn1, crn: crn1] [name: crn2, crn: crn2].",
                failedAuthorizations.get().getAsFailureMessage(AuthorizationResourceAction::getRight, Optional::ofNullable));
        assertFalse(iterator.hasNext());
    }
}