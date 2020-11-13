package com.sequenceiq.authorization.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class AnyMatchTest extends AbstactAuthorizationTest {

    @Test
    public void orReturnsTrueWhenAllTrue() {
        List<AuthorizationRule> authorizations = List.of(mockAuthorization(), mockAuthorization());
        AnyMatch anyMatch = createOr(authorizations);

        List<Boolean> results = createResultWith(anyMatch, true);
        Iterator<Boolean> iterator = results.iterator();

        Optional<AuthorizationRule> failedAuthorizations = anyMatch.evaluateAndGetFailed(iterator);

        assertEquals(Optional.empty(), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void orReturnsFalseWhenAllFalse() {
        List<AuthorizationRule> authorizations = List.of(mockAuthorization(), mockAuthorization());
        AnyMatch anyMatch = createOr(authorizations);

        List<Boolean> results = createResultWith(anyMatch, false);
        Iterator<Boolean> iterator = results.iterator();

        Optional<AuthorizationRule> failedAuthorizations = anyMatch.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(anyMatch), failedAuthorizations);
        assertEquals("You need to have at least one of the following resource rights. Failure. Failure.",
                failedAuthorizations.get().getAsFailureMessage(AuthorizationResourceAction::getRight));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void orReturnsTrueWhenOneTrue() {
        List<AuthorizationRule> authorizations = List.of(mockAuthorization(), mockAuthorization(), mockAuthorization());
        AnyMatch anyMatch = createOr(authorizations);

        List<Boolean> results = List.of(false, true, false);
        Iterator<Boolean> iterator = results.iterator();

        Optional<AuthorizationRule> failedAuthorizations = anyMatch.evaluateAndGetFailed(iterator);

        assertEquals(Optional.empty(), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }
}