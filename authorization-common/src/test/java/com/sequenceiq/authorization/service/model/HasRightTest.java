package com.sequenceiq.authorization.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class HasRightTest {

    @Test
    public void testHasRightSuccess() {
        AuthorizationRule authorization = new HasRight(AuthorizationResourceAction.EDIT_CREDENTIAL, "crn");

        List<Boolean> results = new ArrayList<>();
        authorization.convert((action, crn) -> results.add(true));

        assertEquals(1, results.size());

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.empty(), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHasRightFailure() {
        AuthorizationRule authorization = new HasRight(AuthorizationResourceAction.EDIT_CREDENTIAL, "crn");

        List<Boolean> results = new ArrayList<>();
        authorization.convert((action, crn) -> results.add(false));

        assertEquals(1, results.size());

        Iterator<Boolean> iterator = results.iterator();
        Optional<AuthorizationRule> failedAuthorizations = authorization.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(authorization), failedAuthorizations);
        assertEquals("Doesn't have 'environments/editCredential' right on unknown resource type [name: crn, crn: crn].",
                failedAuthorizations.get().getAsFailureMessage(AuthorizationResourceAction::getRight, Optional::ofNullable));
        assertFalse(iterator.hasNext());
    }
}