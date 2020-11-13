package com.sequenceiq.authorization.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public class AllMatchTest extends AbstactAuthorizationTest {

    @Test
    public void andReturnsTrueWhenAllTrue() {
        List<AuthorizationRule> authorizations = List.of(mockAuthorization(), mockAuthorization());
        AllMatch allMatch = createAnd(authorizations);

        List<Boolean> results = createResultWith(allMatch, true);
        Iterator<Boolean> iterator = results.iterator();

        Optional<AuthorizationRule> failedAuthorizations = allMatch.evaluateAndGetFailed(iterator);

        assertEquals(Optional.empty(), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void andReturnsFalseWhenAllFalse() {
        List<AuthorizationRule> authorizations = List.of(mockAuthorization(), mockAuthorization());
        AllMatch allMatch = createAnd(authorizations);

        List<Boolean> results = createResultWith(allMatch, false);
        Iterator<Boolean> iterator = results.iterator();

        Optional<AuthorizationRule> failedAuthorizations = allMatch.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(allMatch), failedAuthorizations);
        assertEquals("Not authorized for the following reasons. Failure. Failure.",
                failedAuthorizations.get().getAsFailureMessage(AuthorizationResourceAction::getRight));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void andReturnsFalseWhenOneFalse() {
        AuthorizationRule success1 = mockAuthorization();
        AuthorizationRule failed = mockAuthorization();
        AuthorizationRule success2 = mockAuthorization();
        List<AuthorizationRule> authorizations = List.of(success1, failed, success2);
        AllMatch allMatch = createAnd(authorizations);

        List<Boolean> results = List.of(true, false, true);
        Iterator<Boolean> iterator = results.iterator();

        Optional<AuthorizationRule> failedAuthorizations = allMatch.evaluateAndGetFailed(iterator);

        assertEquals(Optional.of(failed), failedAuthorizations);
        assertNotEquals(Optional.of(success1), failedAuthorizations);
        assertNotEquals(Optional.of(success2), failedAuthorizations);
        assertFalse(iterator.hasNext());
    }
}