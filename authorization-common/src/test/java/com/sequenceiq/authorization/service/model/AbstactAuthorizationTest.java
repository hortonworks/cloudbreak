package com.sequenceiq.authorization.service.model;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public abstract class AbstactAuthorizationTest {

    private static final AuthorizationResourceAction ACTION = AuthorizationResourceAction.EDIT_CREDENTIAL;

    private static final String CRN = "crn";

    protected AllMatch createAnd(List<AuthorizationRule> authorizations) {
        Optional<AuthorizationRule> authorization = AllMatch.fromList(authorizations);
        assertTrue(authorization.isPresent());
        return (AllMatch) authorization.get();
    }

    protected AnyMatch createOr(List<AuthorizationRule> authorizations) {
        Optional<AuthorizationRule> authorization = AnyMatch.from(authorizations);
        assertTrue(authorization.isPresent());
        return (AnyMatch) authorization.get();
    }

    protected List<Boolean> createResultWith(AuthorizationRule authorization, boolean value) {
        List<Boolean> result = new ArrayList<>();
        authorization.convert((action, crn) -> result.add(value));
        return result;
    }

    protected AuthorizationRule mockAuthorization() {
        AuthorizationRule authorization = mock(AuthorizationRule.class);
        doAnswer(i -> {
            ((BiConsumer) i.getArgument(0)).accept(ACTION, CRN);
            return null;
        }).when(authorization).convert(any());
        when(authorization.evaluateAndGetFailed(any())).then(i -> {
            boolean next = ((Iterator<Boolean>) i.getArgument(0)).next();
            if (next) {
                return Optional.empty();
            } else {
                return Optional.of(authorization);
            }
        });
        when(authorization.getAsFailureMessage(any())).thenReturn("Failure.");
        return authorization;
    }
}